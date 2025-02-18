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
package com.vivimice.datovn;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vivimice.datovn.build.BuildContext;
import com.vivimice.datovn.build.CompBuild;

public class DatovnApp {

    private static final Logger logger = LoggerFactory.getLogger(DatovnApp.class);
    
    private Path buildDirectory;
    private int parallelism;

    public static void main(String[] args) {
        DatovnApp app = new DatovnApp();
        boolean success = app.buildOnce();
        System.exit(success ? 0 : 1);
    }

    public DatovnApp() {
        logger.debug("Datovn initialized.");

        parallelism = Runtime.getRuntime().availableProcessors(); // Default to the number of available processors
        logger.debug("Parallelism set to: {}", parallelism);
        
        buildDirectory = Path.of(".").toAbsolutePath();
        logger.debug("Build directory: {}", buildDirectory);
    }

    public boolean buildOnce() {
        logger.info("Build started.");

        BuildContext buildContext = new BuildContextImpl();
        try {
            new CompBuild(buildContext).run();
        } catch (DatovnRuntimeException ex) {
            logger.error("Build Failed.", ex);
            return false;
        }

        logger.info("Build Successful.");
        return true;
    }

    private class BuildContextImpl implements BuildContext {

        private final Path actionStoreDirectory = buildDirectory.resolve(".datovn/actions");
        private final ExecutorService compUnitThreadPool = Executors.newWorkStealingPool(parallelism);

        @Override
        public Path getBuildDirectory() {
            return buildDirectory;
        }

        @Override
        public Path getActionStoreDirectory() {
            return actionStoreDirectory;
        }

        @Override
        public ExecutorService getCompUnitThreadPool() {
            return compUnitThreadPool;
        }

    }
    
}
