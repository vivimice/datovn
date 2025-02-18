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
package com.vivimice.datovn.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

public class OffendingPathAccessCheckerTest {

    private Tester newTester() {
        return new Tester();
    }

    @Test
    void testBasic() {
        // test positive offending against same path
        testWithMatrixMap("/foo/bar", "/foo/bar", true, Map.of(
            "mkdir", "mkdir ls rm stat touch read write", 
            "ls",    "mkdir    rm      touch read write",
            "rm",    "mkdir ls rm stat touch read write",
            "stat",  "mkdir    rm      touch           ",
            "touch", "mkdir ls rm stat touch read write",
            "read",  "mkdir ls rm      touch      write",
            "write", "mkdir ls rm      touch read write"
        ));

        // test negative offending against same path
        testWithMatrixMap("/foo/bar", "/foo/bar", false, Map.of(
            "mkdir", "                                 ", 
            "ls",    "      ls    stat                 ",
            "rm",    "                                 ",
            "stat",  "      ls                         ",
            "touch", "                                 ",
            "read",  "            stat       read      ",
            "write", "            stat                 "
        ));

        // test positive offending against existing parent path operations
        testWithMatrixMap("/foo/bar", "/foo", true, Map.of(
            "mkdir", "ls rm",
            "ls",    "   rm",
            "rm",    "ls rm",
            "stat",  "   rm",
            "touch", "ls rm",
            "read",  "   rm",
            "write", "   rm"
        ));

        // test negative offending against existing parent path opeations
        testWithMatrixMap("/foo/bar", "/foo", false, Map.of(
            "mkdir", "     ",
            "ls",    "ls   ",
            "rm",    "     ",
            "stat",  "ls   ",
            "touch", "     ",
            "read",  "ls   ",
            "write", "ls   "
        ));

        // test possitive offending against existing child path operations
        testWithMatrixMap("/foo", "/foo/bar", true, Map.of(
            "ls", "mkdir    rm      touch           ",
            "rm", "mkdir ls rm stat touch read write"
        ));

        // test negative offending against existing child path operations
        testWithMatrixMap("/foo", "/foo/bar", false, Map.of(
            "ls", "      ls    stat       read write",
            "rm", "                                 "
        ));
    }

    private void testWithMatrixMap(String opPath, String existingPath, boolean offendingExpected, Map<String, String> matrix) {
        matrix.forEach((newOp, existingOps) -> {
            for (String existingOp : existingOps.split("\\s+")) {
                if (existingOp.isEmpty()) {
                    continue;
                }
                String message = "new_op='" + newOp + "', existing_op='" + existingOp + "'";
                Consumer<Tester> asserter = (tester) -> {
                    if (offendingExpected) {
                        tester.assertCollision("alice", message + ": offending");
                    } else {
                        tester.assertSuccess(message + ": no_offending");
                    }
                };
                
                asserter.accept(
                    newTester()
                        .su("alice").run(existingOp, existingPath).assertSuccess(message + ": no_offending")
                        .su("bob").run(newOp, opPath)
                );
                asserter.accept(
                    newTester()
                        .su("alice").run(newOp, opPath).assertSuccess(message + ": no_offending")
                        .su("bob").run(existingOp, existingPath)
                );
            }
        });
    }

    private static class Tester {

        String owner;
        OffendingPathAccessChecker<String> checker = new OffendingPathAccessChecker<>();
        PathAccessOperation<String> lastOffendingOperation = null;

        Tester su(String owner) {
            this.owner = owner;
            return this;
        }

        Tester run(String op, String owner) {
            switch (op) {
                case "mkdir":
                    return mkdir(owner);
                case "ls":
                    return ls(owner);
                case "rm":
                    return rm(owner);
                case "stat":
                    return stat(owner);
                case "touch":
                    return touch(owner);
                case "read":
                    return read(owner);
                case "write":
                    return write(owner);
                default:
                    // should never happen
                    throw new AssertionError("unknown op: '" + op + "'");
            }
        }

        Tester mkdir(String path) {
            lastOffendingOperation = checker.onDirectoryCreation(Paths.get(path), owner);
            return this;
        }

        Tester ls(String path) {
            lastOffendingOperation = checker.onDirectoryListing(Paths.get(path), owner);
            return this;
        }

        Tester rm(String path) {
            lastOffendingOperation = checker.onPathRemoval(Paths.get(path), owner);
            return this;
        }

        Tester touch(String path) {
            lastOffendingOperation = checker.onFileCreation(Paths.get(path), owner);
            return this;
        }

        Tester read(String path) {
            lastOffendingOperation = checker.onFileRead(Paths.get(path), owner);
            return this;
        }

        Tester write(String path) {
            lastOffendingOperation = checker.onFileWrite(Paths.get(path), owner);
            return this;
        }

        Tester stat(String path) {
            lastOffendingOperation = checker.onCheckExistence(Paths.get(path), owner);
            return this;
        }

        Tester assertSuccess(String message) {
            assertNull(lastOffendingOperation, message);
            return this;
        }

        Tester assertCollision(String owner, String message) {
            assertNotNull(lastOffendingOperation, message);
            assertEquals(owner, lastOffendingOperation.owner(), message);
            return this;
        }

    }
}