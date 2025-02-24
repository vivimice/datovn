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

import static com.vivimice.datovn.action.MessageLevel.ERROR;
import static com.vivimice.datovn.action.MessageLevel.FATAL;
import static com.vivimice.datovn.action.MessageLevel.WARN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
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

import ch.qos.logback.classic.Level;

public class DatovnTester {

    private static final Logger logger = LoggerFactory.getLogger(DatovnTester.class);
    private static final Level DEFAULT_LOG_LEVEL = Level.WARN;

    private final Path workingDirectory;
    private int passCounter = 0;
    
    public DatovnTester(String caseName) throws IOException {
        Path caseDirectory = Path.of("src/test/resources/cases", caseName);
        workingDirectory = caseDirectory.resolve("working");
        removePathRecursively(workingDirectory);
        copyPathRecursively(caseDirectory.resolve("src"), workingDirectory);
        setLogLevel(DEFAULT_LOG_LEVEL);
    }

    private static void copyPathRecursively(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                try {
                    Files.copy(dir, targetDir, StandardCopyOption.REPLACE_EXISTING);
                } catch (FileAlreadyExistsException ex) {
                    if (!Files.isDirectory(targetDir)) {
                        throw ex;
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public DatovnTester enableTracing() {
        setLogLevel(Level.TRACE);
        return this;
    }

    public DatovnTester disableTracing() {
        setLogLevel(DEFAULT_LOG_LEVEL);
        return this;
    }

    public DatovnTester setLogLevel(ch.qos.logback.classic.Level level) {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME))
            .setLevel(level);
        return this;
    }

    public TestDataManipulator adjustWorkspacePath(String workspacePath) {
        return new TestDataManipulator(workspacePath);
    }

    private static void removePathRecursively(Path p) throws IOException {
        if (Files.exists(p, LinkOption.NOFOLLOW_LINKS)) {
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
        }
    }

    public ResultChecker run() {
        int currentPass = ++passCounter;
        logger.info("DatovnTester build start (Pass #{})", currentPass);

        ResultChecker checker = new ResultChecker();
        checker.buildContext.logProgress(0, "Test Pass #" + currentPass);
        try {
            new CompBuild(checker.buildContext).run();
            checker.executionException = null;
        } catch (DatovnRuntimeException e) {
            checker.executionException = e;
        }
        checker.buildContext.logProgress(1, "Done with Pass #" + currentPass);

        return checker;
    }

    public class TestDataManipulator {

        private final Path target; // in working directory

        public TestDataManipulator(String workspacePath) {
            this.target = workingDirectory.resolve(Path.of(workspacePath));
        }

        public DatovnTester bySetContent(String content) throws IOException {
            Files.write(target, content.getBytes(StandardCharsets.UTF_8));
            return DatovnTester.this;
        }

        public DatovnTester byReplaceAll(String pattern, String replacement) throws IOException {
            String content = new String(Files.readAllBytes(target), StandardCharsets.UTF_8);
            content = content.replaceAll(pattern, replacement);
            Files.write(target, content.getBytes(StandardCharsets.UTF_8));
            return DatovnTester.this;
        }

    }

    public class ResultChecker {

        private DatovnRuntimeException executionException;
        private final TestBuildContextImpl buildContext = new TestBuildContextImpl();

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

        public ResultChecker assertHasMessage(MessageLevel level, Predicate<String> expectedFilter) {
            boolean found = buildContext.messages.computeIfAbsent(level, lv -> new ArrayList<>())
                .stream().anyMatch(expectedFilter);
            assertTrue(found, "expected message not found");
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
                assertTrue(buildContext.errorCounter.get() > 0, "Neither error nor DatovnRuntimeException were reported during build");
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

        private final Path actionStoreDirectory = workingDirectory.resolve(".datovn/actions");
        private final ExecutorService compUnitThreadPool = Executors.newWorkStealingPool();
        private final List<ProfileEvent> events = new ArrayList<>();
        private final AtomicInteger errorCounter = new AtomicInteger(0);
        private final AtomicInteger warningCounter = new AtomicInteger(0);
        private final Map<MessageLevel, List<String>> messages = new ConcurrentHashMap<>();
        private final BuildProfiler profiler = new BuildProfiler(events::add);

        @Override
        public Path getBuildDirectory() {
            return workingDirectory;
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
        public void logProgress(double progress, String message) {
            System.out.println(String.format("[%3.0f%%] %s", progress * 100, message));
        }

        @Override
        public void logMessage(MessageLevel level, String message, String location) {
            List<String> levelMessages = messages.computeIfAbsent(level, lv -> new ArrayList<>());
            synchronized (levelMessages) {
                levelMessages.add(message);
            }
            
            if (level == FATAL || level == ERROR) {
                errorCounter.incrementAndGet();
            } else if (level == WARN) {
                warningCounter.incrementAndGet();
            }

            PrintStream stream = switch (level) {
                case FATAL -> System.err;
                case ERROR -> System.err;
                default -> System.out;
            };

            stream.append("       ");
            stream.append(location).append(" | ");
            if (level != MessageLevel.INFO) {
                stream.append(level.toString()).append(": ");
            }
            stream.append(message);
            stream.println();
        }
    }

}
