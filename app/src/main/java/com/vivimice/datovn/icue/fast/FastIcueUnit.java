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
package com.vivimice.datovn.icue.fast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vivimice.datovn.DatovnRuntimeException;
import com.vivimice.datovn.action.CompAction;
import com.vivimice.datovn.action.MalformedActionDocumentException;
import com.vivimice.datovn.action.SketchDocumentReader;
import com.vivimice.datovn.unit.CompActionRecorder;
import com.vivimice.datovn.unit.CompUnit;
import com.vivimice.datovn.unit.UnitContext;

public class FastIcueUnit implements CompUnit {

    private static final Logger logger = LoggerFactory.getLogger(FastIcueUnit.class);

    private final FastIcueSpec spec;

    public FastIcueUnit(FastIcueSpec spec) {
        assert spec != null : "spec cannot be null";
        this.spec = spec;
    }

    @Override
    public void execute(UnitContext ctx, CompActionRecorder recorder) {
        // We process executable path if it's relative to working directory
        Path executablePath = Path.of(spec.getExecutable());
        if (executablePath.startsWith(".") || executablePath.startsWith("..")) {
            executablePath = ctx.getWorkingDirectory().resolve(executablePath);
        }

        // Prepare command for FastICUE executable
        List<String> command = new ArrayList<>();
        command.add(executablePath.toString());
        command.addAll(spec.getArgs());

        logger.info("Getting FastICUE daemon client for command: {}", command);
        FastIcueClientService clientService = ctx.getStageService(FastIcueClientService.class.getName(), FastIcueClientService::new);
        FastIcueClient client;
        try {
            client = clientService.get(command);
        } catch (FastIcueClientException ex) {
            throw new DatovnRuntimeException("Failed get connection to FastICUE daemon.");
        }

        logger.info("Invoking FastICUE request");
        FastIcueRequest request = new FastIcueRequest("EXEC");
        request.setHeader("Unit", spec.getName());
        request.setHeader("Stage", ctx.getStageName());
        request.setHeader("Opaque-Id", spec.getOpaqueIdentifier());
        request.setHeader("Params-Count", String.valueOf(spec.getParams().size()));
        
        int paramIndex = 0;
        for (String param : spec.getParams()) {
            request.setHeader("Param-Value-" + (paramIndex++), param);
        }

        FastIcueResponse response;
        try {
            response = client.invoke(request);
        } catch (FastIcueInvocationException ex) {
            throw new DatovnRuntimeException("Failed invoking FastICUE request: " + ex.getMessage());
        }

        String statusLine = response.statusCode() + " " + response.statusMessage();
        logger.info("FastICUE response received with status: {}", statusLine);
        if (response.statusCode() != FastIcueConstants.STATUS_ACCEPTED) {
            throw new DatovnRuntimeException("FastICUE request failed with status: " + statusLine);
        }

        logger.info("Parsing sketches from FastICUE response");
        try (SketchDocumentReader reader = new SketchDocumentReader(new BufferedReader(new StringReader(response.data())))) {
             while (true) {
                CompAction.Sketch<?> sketch = reader.read();
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
