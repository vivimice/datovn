/**
 * Copyright 2025 vivimice@gmail.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vivimice.datovn.icue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vivimice.datovn.DatovnRuntimeException;
import com.vivimice.datovn.action.ActionDocumentIterator;
import com.vivimice.datovn.action.ActionDocumentStream;
import com.vivimice.datovn.action.CompAction;
import com.vivimice.datovn.action.MalformedActionDocumentException;
import com.vivimice.datovn.action.MessageLevel;
import com.vivimice.datovn.unit.AbstractCompUnit;
import com.vivimice.datovn.unit.CompActionRecorder;
import com.vivimice.datovn.unit.CompUnit;
import com.vivimice.datovn.unit.UnitContext;

/**
 * Implementation of {@link CompUnit} which executes external executable.
 * 
 * ICUE stands for "Interface of Computation Unit Executable".
 */
public final class IcueUnit extends AbstractCompUnit<IcueSpec> {

    private static final Logger logger = LoggerFactory.getLogger(IcueUnit.class);
    private static final ExecutorService outputStreamDumpers = Executors.newWorkStealingPool();

    public IcueUnit(IcueSpec spec) {
        super(spec);
    }

    @Override
    protected void doCompute(UnitContext ctx, CompActionRecorder recorder) {
        logger.info("Preparing ICUE process ...");

        // Create actions output file which ICUE executable will write to
        Path actionsFile;
        try {
            actionsFile = Files.createTempFile("actions-", ".yml");
        } catch (IOException ex) {
            throw new DatovnRuntimeException("Failed to create temporary actions output file", ex);
        }
        logger.debug("Created actions output file: ", actionsFile);

        // Prepare environment variables for ICUE executable
        Map<String, String> envs = new HashMap<>();
        envs.put("DATOVN_ACTIONS_OUTPUT_FILE", actionsFile.toString());
        envs.put("DATOVN_PARAMS_COUNT", String.valueOf(spec.getParams().size()));
        for (int i = 0; i < spec.getParams().size(); i++) {
            envs.put("DATOVN_PARAM_VALUE_" + i, spec.getParams().get(i));
        }
        logger.debug("ICUE environment variables: {}", envs);

        // Record read file action for ICUE executable path, since we are going to execute it
        String executablePath = ctx.getWorkingDirectory().resolve(spec.getExecutable()).toString();
        recorder.recordReadFile(executablePath);
        
        // Prepare command for ICUE executable
        List<String> command = new ArrayList<>();
        command.add(executablePath);
        command.addAll(spec.getArgs());
        logger.debug("ICUE process command: {}", command);


        // Execute ICUE executable
        Process p;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(ctx.getWorkingDirectory().toFile());
            pb.redirectInput(Redirect.DISCARD);
            pb.environment().putAll(envs);

            logger.info("Starting ICUE process ...");
            p = pb.start();
        } catch (IOException ex) {
            logger.debug("Failed to execute ICUE executable: {}", spec.getKey(), ex);
            recorder.recordFatalError("Failed to execute ICUE executable: " + ex.getMessage());
            return;
        }

        // Redirect messages from ICUE executable during process execution, stdout as INFO, stderr as ERROR
        BiFunction<MessageLevel, InputStream, Runnable> messageRedirectorCreator = (level, inputStream) -> () -> {
            int numOfMessages = 0;
            logger.debug("Dumping ICUE {} messages ...", level);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    synchronized (recorder) {
                        recorder.recordMessage(level, line);
                    }
                    numOfMessages++;
                }
            } catch (IOException ex) {
                logger.error("Failed to dump ICUE {} message: {}", level, spec.getKey(), ex);
            } finally {
                logger.debug("Dumped {} ICUE {} messages", numOfMessages, level);
            }
        };
        Future<?> stdoutRedirector = outputStreamDumpers.submit(
            messageRedirectorCreator.apply(MessageLevel.INFO, p.getInputStream()));
        Future<?> stderrRedirector = outputStreamDumpers.submit(
            messageRedirectorCreator.apply(MessageLevel.ERROR, p.getErrorStream()));

        try {
            stdoutRedirector.get();
            stderrRedirector.get();
        } catch (InterruptedException ex) {
            logger.error("Execution interrupted: {}", spec.getKey(), ex);
            Thread.currentThread().interrupt();
            return;
        } catch (ExecutionException ex) {
            logger.error("Execution i/o error: {}", spec.getKey(), ex);
            throw new DatovnRuntimeException("internal error while redirecting ICUE output streams", ex);
        }

        // Wait for ICUE executable to finish and get its exit code
        int exitCode;
        try {
            exitCode = p.waitFor();
            recorder.recordExit(exitCode);
            logger.debug("ICUE process exited with code: {}", exitCode);
        } catch (InterruptedException ex) {
            logger.error("ICUE external process execution interrupted: {}", spec.getKey(), ex);
            Thread.currentThread().interrupt();
            return;
        }
        
        // Copy actions from temporary file to actionsOutput
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(actionsFile), StandardCharsets.UTF_8))) {
            ActionDocumentIterator<CompAction.Sketch<?>> read = ActionDocumentStream.readSketches(reader);
            while (true) {
                CompAction.Sketch<?> sketch = read.read();
                if (sketch == null) {
                    break;
                }
                recorder.record(sketch);
            }
        } catch (MalformedActionDocumentException ex) {
            throw new DatovnRuntimeException("Malformed action document", ex);
        } catch (IOException ex) {
            throw new DatovnRuntimeException("I/O error while loading action document", ex);
        }
    }

}
