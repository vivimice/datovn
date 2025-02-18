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
package com.vivimice.datovn.stage;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.MDC.MDCCloseable;

import com.vivimice.datovn.action.CompAction;
import com.vivimice.datovn.action.ActionsStore;
import com.vivimice.datovn.action.DirectoryAccessAction;
import com.vivimice.datovn.action.ExecAction;
import com.vivimice.datovn.action.ExitAction;
import com.vivimice.datovn.action.FileAccessAction;
import com.vivimice.datovn.action.MessageOutputAction;
import com.vivimice.datovn.error.MessageLevel;
import com.vivimice.datovn.spec.CompExecSpec;
import com.vivimice.datovn.unit.CompActionRecorder;
import com.vivimice.datovn.unit.CompUnit;
import com.vivimice.datovn.unit.CompUnits;
import com.vivimice.datovn.unit.UnitContext;
import com.vivimice.datovn.util.OffendingPathAccessChecker;
import com.vivimice.datovn.util.PathAccessOperation;

public class CompStage {

    private final static Logger logger = LoggerFactory.getLogger(CompStage.class);

    private final StageContext context;
    private final OffendingPathAccessChecker<String> pathAccessChecker = new OffendingPathAccessChecker<>();
    
    // Variables that must be protected by <code>synchronized (this) {}</code> block
    // Number of remaining unfinished computations
    private int remainComps = 0;

    public CompStage(StageContext context) {
        assert context != null;
        this.context = context;
    }

    /**
     * Start the computation with the initial specification. The specification might
     * execute subsequent specification(s) during the computation. This method will
     * wait for all subsequent specifications to finish.
     * 
     * @param initialSpec the initial specification within this stage
     * @throws IllegalStateException if already started
     * @throws NullPointerException if initialSpec is <code>null</code>
     */
    public void start(CompExecSpec initialSpec) {
        try (MDCCloseable stageMdcc = MDC.putCloseable("stage", context.getStageName())) {
            logger.info("Stage started");
            assert initialSpec != null;
            synchronized (this) {
                // Check if already started
                assert remainComps == 0;

                // Schedule the initial computation
                schedule(initialSpec);

                // Wait for all unfinished computations to finish
                while (remainComps > 0) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            logger.info("Stage finished");
        }
    }

    private void schedule(CompExecSpec spec) {
        assert spec != null;
        logger.debug("Scheduling new exec spec: {}", spec.getKey());

        Executor threadPool = context.getCompUnitThreadPool();
        synchronized (this) {
            remainComps++;
            threadPool.execute(() -> {
                try (MDCCloseable stageMdcc = MDC.putCloseable("stageName", context.getStageName())) {
                    logger.debug("Starting computation ...");
                    try (MDCCloseable unitMdcc = MDC.putCloseable("unitName", spec.getName())) {
                        // execute computation
                        execute(spec);
                    } finally {
                        logger.debug("Computation finished");
                        synchronized (this) {
                            remainComps--;
                            notifyAll();
                        }
                    }
                }
            });
        }
    }

    /**
     * Execute the computation.
     * 
     * @param spec
     */
    private void execute(CompExecSpec spec) {
        // Create computation unit from specification
        CompUnit unit = CompUnits.create(spec);
        UnitContext execContext = new ExecContextImpl(context);
        ActionsStore actionsStore = context.getActionsStore();
        CompActionProcessor actionProcessor = new CompActionProcessor(context, execContext, pathAccessChecker, spec);
        
        boolean shouldReplay;
        logger.debug("Reading previous action sketches");
        List<CompAction.Sketch<?>> prevSketches = actionsStore.loadActionSketches(spec);
        if (prevSketches == null) {
            shouldReplay = false;
            // Execute CompUnit and collect action sketches reported during the computation.
            logger.debug("No action sketches recorded or previous sketches are not valid. Execute computation from scratch.");
            CompActionRecorder actionRecorder = new CompActionRecorder(actionProcessor);
            unit.execute(execContext, actionRecorder);
        } else {
            shouldReplay = true;
            // Replay previous actions
            logger.debug("Replay previous actions.");
            prevSketches.forEach(actionProcessor::accept);
        }

        // Report addition errors during action recording
        for (String error : actionProcessor.processingErrors) {
            execContext.logMessage(MessageLevel.ERROR, error);
        }

        // Write sketches to store, if not replaying.
        if (!shouldReplay) {
            logger.debug("Writing action sketches to store.");
            actionsStore.writeActionSketches(spec, actionProcessor.recordedSketches);
        }

        // Schedule subsequent computations
        logger.debug("Scheduling subsequent computations.");
        for (CompExecSpec subExecSpec : actionProcessor.getInvocations()) {
            schedule(subExecSpec);
        }
    }

    private static class ExecContextImpl implements UnitContext {

        private final StageContext stageContext;

        ExecContextImpl(StageContext stageContext) {
            this.stageContext = stageContext;
        }

        @Override
        public Path getWorkingDirectory() {
            return stageContext.getStageWorkingDir();
        }

        @Override
        public void logMessage(MessageLevel level, String message) {
            PrintStream stream = switch (level) {
                case FATAL -> System.err;
                case ERROR -> System.err;
                default -> System.out;
            };
            stream.println(message);
        }

    }

    private static class CompActionProcessor implements Consumer<CompAction.Sketch<?>> {

        private final List<CompAction.Sketch<?>> recordedSketches = new LinkedList<>();
        private final List<CompExecSpec> invocations = new ArrayList<>();
        private final StageContext stageContext;
        private final UnitContext execContext;
        private final OffendingPathAccessChecker<String> pathAccessChecker;
        private final CompExecSpec spec;
        private final List<String> processingErrors = new ArrayList<>();

        private boolean hasFatalError = false;
        private Optional<Integer> explicitExitCode = Optional.empty();
        private boolean offendingPathAccessReported = false;

        CompActionProcessor(StageContext stageContext, UnitContext execContext, OffendingPathAccessChecker<String> pathAccessChecker, CompExecSpec spec) {
            assert stageContext != null;
            assert execContext != null;
            assert pathAccessChecker != null;
            assert spec != null;

            this.pathAccessChecker = pathAccessChecker;
            this.stageContext = stageContext;
            this.execContext = execContext;
            this.spec = spec;
        }

        @Override
        public synchronized void accept(CompAction.Sketch<?> sketch) {
            recordedSketches.add(sketch);

            if (explicitExitCode.isPresent()) {
                reportProcessingError("no further actions shall be recorded after an explicit exit code has been set.");
            }

            switch (sketch) {
                case ExecAction.Sketch execSketch:
                    // we record exec sketches for subsequent execution.
                    invocations.add(execSketch.getSpec());
                    break;
                    
                case FileAccessAction.Sketch fileAccessSketch:
                    // we check offending path access against operations from other compunits
                    checkPathAccess(fileAccessSketch);
                    break;

                case DirectoryAccessAction.Sketch directoryAccessSketch:
                    // we check offending path access against operations from other compunits
                    checkPathAccess(directoryAccessSketch);
                    break;

                case ExitAction.Sketch exitSketch:
                    explicitExitCode = Optional.of(exitSketch.getExitCode());
                    if (hasFatalError && exitSketch.getExitCode() == 0) {
                        reportProcessingError("Exit code cannot be zero while there are fatal errors reported.");
                    }
                    break;

                case MessageOutputAction.Sketch messageSketch:
                    // we record there was a fatal error
                    hasFatalError |= (messageSketch.getLevel() == MessageLevel.FATAL);
                    // we direct all messages to execution context
                    execContext.logMessage(messageSketch.getLevel(), messageSketch.getMessage());
                    break;

                default:
                   break;
            }
        }

        public List<CompExecSpec> getInvocations() {
            return invocations;
        }

        private void checkPathAccess(FileAccessAction.Sketch sketch) {
            if (offendingPathAccessReported) {
                return;
            }

            checkPathAccess(sketch.getPath(), switch (sketch.getMode()) {
                case CREATE -> pathAccessChecker::onFileCreation;
                case DELETE -> pathAccessChecker::onPathRemoval;
                case READ -> pathAccessChecker::onFileRead;
                case WRITE -> pathAccessChecker::onFileWrite;
                case CHECK_EXISTENCE -> pathAccessChecker::onCheckExistence;
                // should never happen
                default -> (path, owner) -> { 
                    throw new AssertionError(); 
                };
            }, "file access with mode '" + sketch.getMode() + "'");
        }

        private void checkPathAccess(DirectoryAccessAction.Sketch sketch) {
            if (offendingPathAccessReported) {
                return;
            }

            checkPathAccess(sketch.getPath(), switch (sketch.getMode()) {
                case CREATE -> pathAccessChecker::onDirectoryCreation;
                case DELETE -> pathAccessChecker::onPathRemoval;
                case LIST -> pathAccessChecker::onDirectoryListing;
                case CHECK_EXISTENCE -> pathAccessChecker::onCheckExistence;
                // should never happen
                default -> (path, owner) -> { 
                    throw new AssertionError(); 
                };
            }, "directory access with mode '" + sketch.getMode() + "'");
        }

        private void checkPathAccess(String path, BiFunction<Path, String, PathAccessOperation<String>> checker, String description) {
            Path p = stageContext.getStageWorkingDir().resolve(path).toAbsolutePath();

            PathAccessOperation<String> offendingOperation;
            synchronized (pathAccessChecker) { // checker is backed by OffendingPathAccessChecker, which is not thread-safe
                offendingOperation = checker.apply(p, spec.getName());
            }

            if (offendingOperation != null) {
                CompActionRecorder reporter = new CompActionRecorder(this);
                reporter.recordFatalError("Our " + description + " at path '" + p + "' offends operation from '" + offendingOperation.owner() + "': " + offendingOperation.reason());
                offendingPathAccessReported = true;
            }
        }

        private void reportProcessingError(String message) {
            processingErrors.add(message);
        }

    }
}
