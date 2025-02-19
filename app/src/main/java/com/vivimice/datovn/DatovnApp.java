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

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vivimice.datovn.action.MessageLevel;
import com.vivimice.datovn.build.BuildContext;
import com.vivimice.datovn.build.CompBuild;
import com.vivimice.datovn.profiler.BuildProfiler;

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

        BuildContextImpl buildContext = new BuildContextImpl();
        try {
            new CompBuild(buildContext).run();
        } catch (DatovnRuntimeException ex) {
            logger.error("Build failed with fatal error.", ex);
            return false;
        }

        int errorCount = buildContext.errorCounter.get();
        int warningCount = buildContext.warningCounter.get();
        if (errorCount > 0) {
            logger.error("Build failed with {} errors and {} warnings.", errorCount, warningCount);
            return false;
        }
        
        if (warningCount > 0) {
            logger.info("Build successful with {} warnings.", warningCount);
        } else {
            logger.info("Build successful.");
        }
        
        return true;
    }

    private class BuildContextImpl implements BuildContext {

        private final Path actionStoreDirectory = buildDirectory.resolve(".datovn/actions");
        private final ExecutorService compUnitThreadPool = Executors.newWorkStealingPool(parallelism);
        private final BuildProfiler profiler = new BuildProfiler((event) -> {});
        private final AtomicInteger errorCounter = new AtomicInteger(0);
        private final AtomicInteger warningCounter = new AtomicInteger(0);

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

        @Override
        public BuildProfiler getProfiler() {
            return profiler;
        }

        @Override
        public void logMessage(MessageLevel level, String message) {
            PrintStream stream;
            switch (level) {
                case FATAL:
                case ERROR:
                    stream = System.err;
                    errorCounter.incrementAndGet();
                    break;
                case WARN:
                    stream = System.err;
                    warningCounter.incrementAndGet();
                    break;
                default:
                    stream = System.out;
            };
            stream.println(message);
        }

    }
    
}
