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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vivimice.datovn.action.MessageLevel;
import com.vivimice.datovn.build.BuildContext;
import com.vivimice.datovn.build.CompBuild;
import com.vivimice.datovn.profiler.BuildProfiler;
import com.vivimice.datovn.profiler.ProfileEvent;

public class DatovnTester {

    private static final Logger logger = LoggerFactory.getLogger(DatovnTester.class);

    private final Path caseDirectory;
    private final TestBuildContextImpl buildContext;
    
    private boolean preserveActions = false;
    private DatovnRuntimeException executionException;

    public DatovnTester(String caseName) {
        caseDirectory = Path.of("src/test/resources/cases", caseName);
        buildContext = new TestBuildContextImpl();
    }

    public DatovnTester preserveActions() {
        preserveActions = true;
        return this;
    }

    private static void removePathRecursively(Path p) {
        if (Files.exists(p, LinkOption.NOFOLLOW_LINKS)) {
            try {
                Files.walkFileTree(p, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }
    
                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        if (exc != null) {
                            throw exc;
                        }
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException ex) {
                throw new RuntimeException("Failed remove path: " + p, ex);
            }
        }
    }

    public ResultChecker run() {
        logger.info("DatovnTester build start.");

        if (!preserveActions) {
            removePathRecursively(buildContext.actionStoreDirectory);
        }

        try {
            new CompBuild(buildContext).run();
            executionException = null;
        } catch (DatovnRuntimeException e) {
            executionException = e;
        }

        return new ResultChecker();
    }

    public class ResultChecker {

        public ResultChecker assertSuccess() {
            assertNull(executionException, "DatovnRuntimeException was thrown during build");
            assertWithErrors(0);
            return this;
        }

        public ResultChecker assertWithWarnings(int expectedWarnings) {
            assertEquals(expectedWarnings, buildContext.warningCounter.get());
            return this;
        }

        public ResultChecker assertWithErrors(int expectedErrors) {
            assertEquals(expectedErrors, buildContext.errorCounter.get());
            return this;
        }

        public ResultChecker assertHasMessage(MessageLevel level, String expectedMessage) {
            boolean found = buildContext.messages.computeIfAbsent(level, lv -> new ArrayList<>()).contains(expectedMessage);
            assertTrue(found, "expected message not found");
            return this;
        }

        public ResultChecker assertNoEvent(String name) {
            boolean found = buildContext.events.stream().anyMatch(
                event -> event.name().equals(name));
            assertTrue(!found, "unexpected event found");
            return this;
        }

        public ResultChecker assertHasEvent(String expectedName, Predicate<Map<String, Object>> dataFilter) {
            boolean found = buildContext.events.stream().anyMatch(
                event -> event.name().equals(expectedName) && dataFilter.test(event.data()));
            assertTrue(found, "expected event not found");
            return this;
        }

        public ResultChecker assertHasEvent(String expectedName) {
            assertHasEvent(expectedName, data -> true);
            return this;
        }

        public ResultChecker assertFailure() {
            if (executionException == null) {
                assertTrue(buildContext.errorCounter.get() > 0, "Neighther error nor DatovnRuntimeException were reported during build");
            }
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
        private final List<ProfileEvent> events = new ArrayList<>();
        private final AtomicInteger errorCounter = new AtomicInteger(0);
        private final AtomicInteger warningCounter = new AtomicInteger(0);
        private final Map<MessageLevel, List<String>> messages = new ConcurrentHashMap<>();
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

        @Override
        public void logMessage(MessageLevel level, String message) {
            List<String> levelMessages = messages.computeIfAbsent(level, lv -> new ArrayList<>());
            synchronized (levelMessages) {
                levelMessages.add(message);
                System.out.println("[" + level + "] " + message);
            }

            switch (level) {
                case FATAL:
                case ERROR:
                    errorCounter.incrementAndGet();
                    break;
                case WARN:
                    warningCounter.incrementAndGet();
                    break;
                default:
                    break;
            };
        }
    }

}
