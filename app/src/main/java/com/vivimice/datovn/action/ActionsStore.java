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
package com.vivimice.datovn.action;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.vivimice.datovn.DatovnRuntimeException;
import com.vivimice.datovn.build.BuildContext;
import com.vivimice.datovn.spec.CompExecSpec;
import com.vivimice.datovn.stage.StageContext;

public class ActionsStore {

    private static final Logger logger = LoggerFactory.getLogger(ActionsStore.class);

    private static final String CURRENT_FILE_VERSION = "v1";
    private static final ObjectMapper actionsMapper = new ObjectMapper(new YAMLFactory());

    private final Path stageDirectory;
    private final Path storeDirectory;

    public ActionsStore(BuildContext buildContext, StageContext stageContext) {
        this.stageDirectory = stageContext.getStageWorkingDir();
        this.storeDirectory = buildContext.getActionStoreDirectory().resolve(stageContext.getStageName());

        try {
            Files.createDirectories(this.storeDirectory);
        } catch (IOException ex) {
            throw new DatovnRuntimeException("i/o error while create directory for action store: " + this.storeDirectory, ex);
        }
    }

    private Path getActionsFile(CompExecSpec spec) {
        return storeDirectory.resolve(spec.getName() + ".actions.yml");
    }

    /**
     * Write action sketches of a computation unit (created by the specified specification) to the store.
     * 
     * @param context the stage context within which the computation unit is executed
     * @param spec the specification of the computation unit
     * @param sketches the action sketches collected during the execution of the computation unit
     */
    public void writeActionSketches(CompExecSpec spec, List<CompAction.Sketch<?>> sketches) {
        ActionPathMappingContext convertContext = new ActionPathMappingContext(stageDirectory, storeDirectory);

        List<CompAction> actions = new ArrayList<>(sketches.size());
        for (CompAction.Sketch<?> sketch : sketches) {
            actions.add(sketch.toAction(convertContext));
        }

        Map<String, Object> data = Map.of(
            "version", CURRENT_FILE_VERSION,
            "actions", actions
        );

        Path actionsFile = getActionsFile(spec);
        logger.debug("Writing actions to: {}", actionsFile);
        actionsFile.getParent().toFile().mkdirs();

        try {
            actionsMapper.writeValue(actionsFile.toFile(), data);
        } catch (IOException ex) {
            throw new DatovnRuntimeException("i/o error while writing action to file: " + actionsFile, ex);
        }
    }

    /**
     * Read action sketches of a computation unit (created by the specified specification) from the store.
     * 
     * Validation will be performed on the loaded action sketches. If validation fails, null will be returned.
     * 
     * By "validation", we mean that the actions are checked against the current state of the file system or whatsoever is relevant.
     * 
     * @param context
     * @param spec
     * @return the action sketches collected during the execution of the computation unit. null if no such sketches exist.
     */
    public List<CompAction.Sketch<?>> loadActionSketches(CompExecSpec spec) {
        ActionPathMappingContext mappingContext = new ActionPathMappingContext(stageDirectory, storeDirectory);

        Path actionsFile = getActionsFile(spec);
        logger.debug("Loading actions from: {}", actionsFile);
        if (!Files.exists(actionsFile)) {
            logger.debug("Actions file not found: ", actionsFile);
            return null;
        }

        Map<String, Object> data;
        try {
            data = actionsMapper.readValue(actionsFile.toFile(), new TypeReference<Map<String, Object>>() {});
        } catch (IOException ex) {
            throw new DatovnRuntimeException("i/o error while reading action from file: " + actionsFile, ex);
        }

        if (Objects.equals(data.get("version"), CURRENT_FILE_VERSION)) {
            throw new DatovnRuntimeException("action file version is not compatible with the current version '" + CURRENT_FILE_VERSION + "'. File: " + actionsFile);
        }

        List<CompAction> actions;
        try {
            actions = actionsMapper.convertValue(data.get("actions"), new TypeReference<List<CompAction>>() {});
        } catch (IllegalArgumentException ex) {
            throw new DatovnRuntimeException("malformed action data in file: " + actionsFile, ex);
        }

        // validate the actions
        logger.debug("Validating actions...");
        for (CompAction action : actions) {
            boolean upToDate = action.isUpToDate(mappingContext);
            if (!upToDate) {
                logger.debug("Outdated action found.");
                return null;
            }
        }

        List<CompAction.Sketch<?>> sketches = new ArrayList<>(actions.size());
        for (CompAction action : actions) {
            sketches.add(action.toSketch(mappingContext));
        }
        
        return sketches;
    }

}
