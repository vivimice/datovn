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
package com.vivimice.datovn.build;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executor;

import com.vivimice.datovn.DatovnRuntimeException;
import com.vivimice.datovn.action.ActionsStore;
import com.vivimice.datovn.action.MessageLevel;
import com.vivimice.datovn.profiler.ProfilerCloseable;
import com.vivimice.datovn.profiler.StageProfiler;
import com.vivimice.datovn.stage.CompStage;
import com.vivimice.datovn.stage.StageContext;
import com.vivimice.datovn.stage.bootstrap.StageBootstrapSpec;

public class CompBuild {

    private final BuildContext context;

    public CompBuild(BuildContext context) {
        assert context != null;
        this.context = context;
    }

    public void run() {
        Path buildDirectory = context.getBuildDirectory();
        if (!Files.isDirectory(buildDirectory)) {
            throw new DatovnRuntimeException("build dir is not a directory: " + buildDirectory);
        }

        try (ProfilerCloseable pc = context.getProfiler().wrapBuild()) {
            // every directory under the build directory corresponding to a stage
            Files.list(buildDirectory).forEach(p -> {
                if (p.getFileName().toString().startsWith(".")) {
                    // we skip hidden directories (e.g. .git) and files
                    return;
                }

                if (Files.isDirectory(p)) {
                    runStage(p);
                }
            });
        } catch (IOException ex) {
            throw new DatovnRuntimeException("i/o error while listing build dir: " + buildDirectory);
        }
    }

    private void runStage(Path stageDir) {
        StageContext stageContext = new StageContextImpl(stageDir);
        CompStage stage = new CompStage(stageContext);
        stage.start(new StageBootstrapSpec());
    }

    private class StageContextImpl implements StageContext {

        private final StageProfiler profiler = context.getProfiler().createStageProfiler();
        private final Path stageDirectory;
        private final String name;

        public StageContextImpl(Path stageDirectory) {
            this.name = stageDirectory.getFileName().toString();
            this.stageDirectory = stageDirectory;
        }

        @Override
        public ActionsStore getActionsStore() {
            return new ActionsStore(context, this);
        }

        @Override
        public Executor getCompUnitThreadPool() {
            return context.getCompUnitThreadPool();
        }

        @Override
        public String getStageName() {
            return name;
        }

        @Override
        public Path getStageWorkingDir() {
            return stageDirectory;
        }

        @Override
        public StageProfiler getProfiler() {
            return profiler;
        }

        @Override
        public void logMessage(MessageLevel level, String message) {
            context.logMessage(level, message);
        }

    }
    
}
