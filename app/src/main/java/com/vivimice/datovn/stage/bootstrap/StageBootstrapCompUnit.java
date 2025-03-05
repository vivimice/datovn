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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.vivimice.datovn.spec.CompExecSpec;
import com.vivimice.datovn.unit.CompActionRecorder;
import com.vivimice.datovn.unit.CompUnit;
import com.vivimice.datovn.unit.CompUnits;
import com.vivimice.datovn.unit.UnitContext;
import com.vivimice.datovn.util.InjectableJsonLocationValues;
import com.vivimice.datovn.util.JsonUtils;

public class StageBootstrapCompUnit implements CompUnit {

    private static final Logger logger = LoggerFactory.getLogger(StageBootstrapCompUnit.class);
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private static final String CONFIG_FILENAME = "stage.yml";

    static {
        mapper.setInjectableValues(new InjectableJsonLocationValues());
    }

    @Override
    public void execute(UnitContext ctx, CompActionRecorder recorder) {
        Path path = ctx.getWorkingDirectory().resolve(CONFIG_FILENAME);

        recorder.recordCheckFileExists(CONFIG_FILENAME);
        if (!Files.exists(path)) {
            recorder.recordWarning("not found", path.toString());
            return;
        } else if (!Files.isRegularFile(path)) {
            recorder.recordError("not a file", path.toString());
            return;
        }

        recorder.recordReadFile(CONFIG_FILENAME);
        StageBootstrapDescriptor descriptor;
        try {
            descriptor = mapper.readValue(path.toFile(), StageBootstrapDescriptor.class);
        } catch (JsonProcessingException ex) {
            recorder.recordError(ex.getOriginalMessage(), path.toString());
            return;
        } catch (IOException ex) {
            logger.warn("i/o error reading bootstram config: {}", path, ex);
            recorder.recordError(ex.getMessage(), path.toString());
            return;
        }

        List<UnitDescriptor> units = descriptor.getUnits();
        if (units != null) {
            for (UnitDescriptor unit : descriptor.getUnits()) {
                String declaration = JsonUtils.formatJsonLocation(unit.getLocation(), path.toString());
                try {
                    unit.afterMapping(mapper);
                } catch (IllegalArgumentException ex) {
                    recorder.recordError(ex.getMessage(), declaration);
                    continue;
                }

                CompExecSpec spec = CompUnits.createSpec(unit);
                if (spec != null) {
                    recorder.recordInfo("Scheduled unit: " + spec.getName());
                    recorder.recordExec(spec);
                } else {
                    recorder.recordError("Unsupported unit type: " + unit.getClass().getName(), declaration);
                }
            }
        }
    }

}
