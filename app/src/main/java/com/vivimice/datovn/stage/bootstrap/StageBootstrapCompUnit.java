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
package com.vivimice.datovn.stage.bootstrap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.vivimice.datovn.spec.CompExecSpec;
import com.vivimice.datovn.unit.AbstractCompUnit;
import com.vivimice.datovn.unit.CompActionRecorder;
import com.vivimice.datovn.unit.CompUnits;
import com.vivimice.datovn.unit.UnitContext;

public class StageBootstrapCompUnit extends AbstractCompUnit<StageBootstrapSpec> {

    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private static final String CONFIG_FILENAME = "stage.yml";

    public StageBootstrapCompUnit(StageBootstrapSpec spec) {
        super(spec);
    }

    @Override
    protected void doCompute(UnitContext ctx, CompActionRecorder recorder) {
        Path path = ctx.getWorkingDirectory().resolve(CONFIG_FILENAME);

        recorder.recordCheckFileExists(CONFIG_FILENAME);
        if (!Files.exists(path)) {
            recorder.recordWarning("not found: " + path);
            return;
        } else if (!Files.isRegularFile(path)) {
            recorder.recordError("not a file: " + path);
            return;
        }

        recorder.recordReadFile(CONFIG_FILENAME);
        StageBootstrapDescriptor descriptor;
        try {
            descriptor = mapper.readValue(path.toFile(), StageBootstrapDescriptor.class);
        } catch (JsonProcessingException ex) {
            recorder.recordError("malformed: " + path + ". cause: " + ex.getMessage());
            return;
        } catch (IOException ex) {
            recorder.recordError("i/o error while reading: " + path + ". error: " + ex.getMessage());
            return;
        }

        List<UnitDescriptor> units = descriptor.getUnits();
        if (units != null) {
            for (UnitDescriptor unit : descriptor.getUnits()) {
                unit.afterMapping(mapper);
                CompExecSpec spec = CompUnits.createSpec(unit);
                if (spec != null) {
                    recorder.recordInfo("Scheduled unit: " + spec.getName());
                    recorder.recordExec(spec);
                } else {
                    recorder.recordError("Unsupported unit type: " + unit.getClass().getName());
                }
            }
        }
    }

}
