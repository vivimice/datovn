/*
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

import com.vivimice.datovn.build.BuildContext;
import com.vivimice.datovn.build.CompBuild;
import com.vivimice.datovn.profiler.BuildProfiler;
import com.vivimice.datovn.profiler.ProfileEvent;

public class DatovnTester {

    private final Path caseDirectory;
    
    private boolean buildSuccessful;
    private List<ProfileEvent> events = new ArrayList<>();
    private DatovnRuntimeException executionException;

    public DatovnTester(String caseName) {
        this.caseDirectory = Path.of("src/test/resources/cases", caseName);
    }

    public ResultChecker run() {
        try {
            new CompBuild(new TestBuildContextImpl()).run();
            executionException = null;
            buildSuccessful = true;
        } catch (DatovnRuntimeException e) {
            executionException = e;
            buildSuccessful = false;
        }

        return new ResultChecker();
    }

    public class ResultChecker {

        public ResultChecker assertSuccess() {
            assertTrue(buildSuccessful, "Build failed unexpectedly");
            return this;
        }

        public ResultChecker assertFailure() {
            assertFalse(buildSuccessful, "Build succeeded unexpectedly");
            return this;
        }

        public ResultChecker assertExceptionMatches(Predicate<String> pred) {
            assertNotNull(executionException, "No DatovnRuntimeException was thrown");
            String message = executionException.getMessage();
            assertTrue(pred.test(message), "Exception message did not match. message: " + message);
            return this;
        }

    }

    private class TestBuildContextImpl implements BuildContext {
        private final Path actionStoreDirectory = caseDirectory.resolve(".datovn/actions");
        private final ExecutorService compUnitThreadPool = Executors.newWorkStealingPool();
        private final BuildProfiler profiler = new BuildProfiler(events::add);

        @Override
        public Path getBuildDirectory() {
            return caseDirectory;
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
    }

}
