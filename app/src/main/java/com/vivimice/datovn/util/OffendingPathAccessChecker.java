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
package com.vivimice.datovn.util;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static com.vivimice.datovn.util.OperationType.*;

/**
 * This class is used to check possible path access collisions between different CompUnits of the same stage. A path
 * access collision happens when two or more CompUnits taking path access action might interfere with each other. Like:
 * 
 *   - CompUnit A reads a file and CompUnit B writes to the same file
 *   - CompUnit A create directory /foo/bar/ and CompUnit B deletes /foo/
 *   - CompUnit A list directory /foo/ and CompUnit B reads /foo/bar/baz.txt
 * 
 * # OPERATIONS
 * 
 * Every SUCCESSFUL path access action can be considered as one or several operations, which can be categorized as
 * 
 *   - CONTENT_READ and CONTENT_WRITE: read from or write to a file, respectively.
 *   - PATH_CHECK: check existence of a path
 *   - DIR_CREATE and FILE_CREATE: create a directory or a file, respectively. 
 *   - DIR_LIST: list a directory 
 *   - PATH_DELETE: remove the directory itself.
 * 
 * Every successful path access involved access a path itself and its parent path. Like: create directory at /foo/bar,
 * we not only on /foo/bar itself, but alse implies that /foo exists. So we can record and test operations on the path. 
 * If a operation might affect or be affected by any subsequent operations on its sub path, like directory listing and 
 * recursive directory removal, then we record the operation on path prefix too.
 * 
 * # PATH ACCESS OPERATIONS
 * 
 * We record operations for each path access by follows table:
 * 
 * +------------------------+---------------+----------------+-----------------------+
 * | Successful Path Access | Operation     | Record on Path | Record on Path Prefix |
 * +------------------------+---------------+----------------+-----------------------+
 * | on directory creation  | DIR_CREATE    | Y              |                       |    
 * | on directory listing   | DIR_LIST      | Y              | Y                     |
 * | on directory removal   | PATH_DELETE   | Y              | Y                     |
 * | on check existence     | PATH_CHECK    | Y              |                       |
 * | on file creation       | FILE_CREATE   | Y              |                       |
 * | on file read           | CONTENT_READ  | Y              |                       |
 * | on file write          | CONTENT_WRITE | Y              |                       |
 * +------------------------+---------------+----------------+-----------------------+
 * 
 * # COLLISION DETECTION
 * 
 * Collisions are detected when operations are being recorded on a path or a path prefix. 
 * 
 * Say the one operation are being recorded, we first get previous operations recorded on the path, then get all operations 
 * recorded on EVERY path prefixes that prefixes the path. 
 * 
 * The following table shows possible offending operations, which is initiated by different owners, recorded on P 
 * (X means offended):
 * 
 * +-------------------------------+----------+-------------+------------+-------------+--------------+---------------+
 * | Any existing Op.              |          |             |            |             |              |               | 
 * +----------------+   DIR_CREATE | DIR_LIST | PATH_DELETE | PATH_CHECK | FILE_CREATE | CONTENT_READ | CONTENT_WRITE |
 * | New Op.         \             |          |             |            |             |              |               |
 * |                  +------------+----------+-------------+------------+-------------+--------------+---------------+
 * | DIR_CREATE       |          X |        X |           X |          X |           X |            X |             X |
 * | DIR_LIST         |          X |          |           X |            |           X |            X |             X |
 * | PATH_DELETE      |          X |        X |           X |          X |           X |            X |             X |
 * | PATH_CHECK       |          X |          |           X |            |           X |              |               |
 * | FILE_CREATE      |          X |        X |           X |          X |           X |            X |             X |
 * | CONTENT_READ     |          X |        X |           X |            |           X |              |             X |
 * | CONTENT_WRITE    |          X |        X |           X |            |           X |            X |             X |
 * +------------------+------------+----------+-------------+------------+-------------+--------------+---------------+
 * 
 * The following table shows possible offending operations, which is initiated by different owners, recorded on 
 * every path prefix that prefixes the path (X means offended):
 * 
 * +---------------------------+-------------+
 * | Any parent Op.            |             | 
 * +--------------+   DIR_LIST | PATH_DELETE |
 * | New Op.       \           |             |
 * |                +----------+-------------+
 * | DIR_CREATE     |        X |           X |
 * | DIR_LIST       |          |           X |
 * | PATH_DELETE    |        X |           X |
 * | PATH_CHECK     |          |           X |
 * | FILE_CREATE    |        X |           X |
 * | CONTENT_READ   |          |           X |
 * | CONTENT_WRITE  |          |           X |
 * +----------------+----------+-------------+
 * 
 * For certain operations, we may also need to check the children and grand children of a path, as well as the parent, not
 * offending the new operation we are about to record, the following table shows possible offending operations, which is 
 * initiated by different owners, recorded on every children and grand children of the path (X means offended):
 * 
 * +----------------------------+----------+-------------+------------+-------------+--------------+---------------+
 * | Any child Op.              |          |             |            |             |              |               | 
 * +-------------+   DIR_CREATE | DIR_LIST | PATH_DELETE | PATH_CHECK | FILE_CREATE | CONTENT_READ | CONTENT_WRITE |
 * | New Op.      \             |          |             |            |             |              |               |
 * |               +------------+----------+-------------+------------+-------------+--------------+---------------+
 * | DIR_LIST      |          X |          |           X |            |           X |              |               |
 * | PATH_DELETE   |          X |        X |           X |          X |           X |            X |             X |
 * +---------------+------------+----------+-------------+------------+-------------+--------------+---------------+
 * 
 * # THREAD SAFTY
 * 
 * This class is NOT thread-safe.
 */
public class OffendingPathAccessChecker<T> {

    private static class TreeNode<T> {
        final Map<Path, TreeNode<T>> children = new HashMap<>();
        final List<PathAccessOperation<T>> operations = new ArrayList<>();
        final List<PathAccessOperation<T>> prefixOperations = new ArrayList<>();
        final Path path;

        TreeNode(Path path) {
            this.path = path;
        }

        TreeNode<T> copy() {
            TreeNode<T> copy = new TreeNode<>(path);
            copy.operations.addAll(operations);
            copy.prefixOperations.addAll(prefixOperations);
            return copy;
        }
    }

    private static final int NUM_OF_OPERATIONS = OperationType.values().length;
    private static final boolean[][] selfMatrix = new boolean[NUM_OF_OPERATIONS][];
    private static final boolean[][] prefixMatrix = new boolean[NUM_OF_OPERATIONS][];
    private static final boolean[][] childMatrix = new boolean[NUM_OF_OPERATIONS][];
    static {
        define(selfMatrix).on(DIR_CREATE).offendedBy(
            DIR_CREATE, DIR_LIST, PATH_DELETE, PATH_CHECK, FILE_CREATE, CONTENT_READ, CONTENT_WRITE
        );

        define(selfMatrix).on(DIR_LIST).offendedBy(
            DIR_CREATE, PATH_DELETE, FILE_CREATE, CONTENT_READ, CONTENT_WRITE
        );

        define(selfMatrix).on(PATH_DELETE).offendedBy(
            DIR_CREATE, DIR_LIST, PATH_DELETE, PATH_CHECK, FILE_CREATE, CONTENT_READ, CONTENT_WRITE
        );

        define(selfMatrix).on(PATH_CHECK).offendedBy(
            DIR_CREATE, PATH_DELETE, FILE_CREATE
        );

        define(selfMatrix).on(FILE_CREATE).offendedBy(
            DIR_CREATE, DIR_LIST, PATH_DELETE, PATH_CHECK, FILE_CREATE, CONTENT_READ, CONTENT_WRITE
        );

        define(selfMatrix).on(CONTENT_READ).offendedBy(
            DIR_CREATE, DIR_LIST, PATH_DELETE, FILE_CREATE, CONTENT_WRITE
        );

        define(selfMatrix).on(CONTENT_WRITE).offendedBy(
            DIR_CREATE, DIR_LIST, PATH_DELETE, FILE_CREATE, CONTENT_READ, CONTENT_WRITE
        );

        /////////////////////////////////////////

        define(prefixMatrix).on(DIR_CREATE).offendedBy(
            DIR_LIST, PATH_DELETE
        );

        define(prefixMatrix).on(DIR_LIST).offendedBy(
            PATH_DELETE
        );

        define(prefixMatrix).on(PATH_DELETE).offendedBy(
            DIR_LIST, PATH_DELETE
        );

        define(prefixMatrix).on(PATH_CHECK).offendedBy(
            PATH_DELETE
        );

        define(prefixMatrix).on(FILE_CREATE).offendedBy(
            DIR_LIST, PATH_DELETE
        );

        define(prefixMatrix).on(CONTENT_READ).offendedBy(
            PATH_DELETE
        );

        define(prefixMatrix).on(CONTENT_WRITE).offendedBy(
            PATH_DELETE
        );

        define(childMatrix).on(CONTENT_READ).offendedBy(
            PATH_DELETE
        );

        /////////////////////////////////////////

        define(childMatrix).on(DIR_LIST).offendedBy(
            DIR_CREATE, PATH_DELETE, FILE_CREATE
        );

        define(childMatrix).on(PATH_DELETE).offendedBy(
            DIR_CREATE, DIR_LIST, PATH_DELETE, PATH_CHECK, FILE_CREATE, CONTENT_READ, CONTENT_WRITE
        );

    }

    private static interface OperationCollisions {
        void offendedBy(OperationType... types);
    }

    private static interface CollisionMatrixRegistry {
        OperationCollisions on(OperationType newOpType);
    }

    private static CollisionMatrixRegistry define(boolean[][] matrix) {
        return (newOpType) -> (collisionTypes) -> {
            boolean[] row = matrix[newOpType.ordinal()];
            if (row == null) {
                row = new boolean[NUM_OF_OPERATIONS];
                matrix[newOpType.ordinal()] = row;
            }
            for (OperationType t : collisionTypes) {
                row[t.ordinal()] = true;
            }
        };
    }

    private final TreeNode<T> root;

    public OffendingPathAccessChecker() {
        this.root = new TreeNode<>(null);
    }

    /**
     * Copy constructor.
     * @param source The source to copy from.
     */
    public OffendingPathAccessChecker(OffendingPathAccessChecker<T> source) {
        this.root = source.root.copy();
    }

    private PathAccessOperation<T> isOffending(PathAccessOperation<T> x) {
        PathAccessOperation<T> offendingOp = null;

        // traverse the tree to find any conflicting operations
        Path path = x.path();
        int nameCount = path.getNameCount();
        TreeNode<T> node = root;
        for (int i = 0; i < nameCount; i++) {
            Path p = path.subpath(0, i + 1);
            node = node.children.get(p);
            if (node == null) {
                break;
            }

            if (i < nameCount - 1) {
                // This is parent of x.path(), so we lookup
                // lookup for any conflicting operations that are not from the same owner
                offendingOp = findAnyOffendingOperation(x, node.prefixOperations.stream(), prefixMatrix);
                if (offendingOp != null) {
                    return offendingOp;
                }
            } else {
                // operation on current node, we check path itself
                offendingOp = findAnyOffendingOperation(x, node.operations.stream(), selfMatrix);
                if (offendingOp != null) {
                    return offendingOp;
                }
            }
        }

        if (node != null) {
            // we have children to lookup
            Deque<TreeNode<T>> children = new LinkedList<>(node.children.values());
            while (!children.isEmpty()) {
                TreeNode<T> child = children.removeFirst();
                // lookup for any conflicting operations that are not from the same owner
                offendingOp = findAnyOffendingOperation(x, child.operations.stream(), childMatrix);
                if (offendingOp != null) {
                    return offendingOp;
                }
                
                children.addAll(child.children.values());
            }
        }
        
        return null;
    }

    private PathAccessOperation<T> findAnyOffendingOperation(
        PathAccessOperation<T> x, 
        Stream<PathAccessOperation<T>> stream, 
        boolean[][] collisionMatrix
    ) {
        return stream
            .filter(op -> !Objects.equals(op.owner(), x.owner())) 
            .filter(op -> collisionMatrix[x.type().ordinal()][op.type().ordinal()])
            .findAny()
            .orElse(null);
    }

    private PathAccessOperation<T> onPathAccess(PathAccessOperation<T> op) {
        assert op.path().isAbsolute() : "Path must be absolute";
        assert op.owner() != null : "Owner must not be null";
        assert op.reason() != null : "Reason must not be null";

        // check collision
        PathAccessOperation<T> offendingOp = this.isOffending(op);
        if (offendingOp != null) {
            return offendingOp; 
        }

        OperationType type = op.type();
        Path path = op.path();
        TreeNode<T> node = root;
        int nameCount = path.getNameCount();
        for (int i = 0; i < nameCount; i++) {
            Path p = path.subpath(0, i + 1);
            node = node.children.computeIfAbsent(p, TreeNode::new);
            if (i == nameCount - 1) {
                // path itself
                node.operations.add(op);
                // prefix operations for delete and list operations
                if (type == PATH_DELETE || type == DIR_LIST) {
                    node.prefixOperations.add(op);
                }
            }
        }

        return null;
    }

    /**
     * Check offending operations upon successful directory creation. 
     * 
     * Return the offending operation if any. Otherwise, return null.
     */
    public PathAccessOperation<T> onDirectoryCreation(Path p, T owner) {
        String reason = "directory creation at " + p;
        return onPathAccess(new PathAccessOperation<T>(DIR_CREATE, p, owner, reason));
    }

    /**
     * Check offending operations upon successful directory listing. 
     * 
     * Return the offending operation if any. Otherwise, return null.
     */
    public PathAccessOperation<T> onDirectoryListing(Path p, T owner) {
        String reason = "directory listing at " + p;
        return onPathAccess(new PathAccessOperation<T>(DIR_LIST, p, owner, reason));
    }

    /**
     * Check offending operations upon successful directory removal. 
     * 
     * Return the offending operation if any. Otherwise, return null.
     */
    public PathAccessOperation<T> onPathRemoval(Path p, T owner) {
        String reason = "directory removal at " + p;
        return onPathAccess(new PathAccessOperation<T>(PATH_DELETE, p, owner, reason));
    }

    /**
     * Check offending operations upon successful existence check. 
     * 
     * Return the offending operation if any. Otherwise, return null.
     */
    public PathAccessOperation<T> onCheckExistence(Path p, T owner) {
        String reason = "existence checking at " + p;
        return onPathAccess(new PathAccessOperation<T>(PATH_CHECK, p, owner, reason));
    }

    /**
     * Check offending operations upon successful file creation. 
     * 
     * Return the offending operation if any. Otherwise, return null.
     */
    public PathAccessOperation<T> onFileCreation(Path p, T owner) {
        String reason = "file creation at " + p;
        return onPathAccess(new PathAccessOperation<T>(FILE_CREATE, p, owner, reason));
    }

    /**
     * Check offending operations upon successful file read. 
     * 
     * Return the offending operation if any. Otherwise, return null.
     */
    public PathAccessOperation<T> onFileRead(Path p, T owner) {
        String reason = "file read at " + p;
        return onPathAccess(new PathAccessOperation<T>(CONTENT_READ, p, owner, reason));
    }

        /**
     * Check offending operations upon successful file write. 
     * 
     * Return the offending operation if any. Otherwise, return null.
     */
    public PathAccessOperation<T> onFileWrite(Path p, T owner) {
        String reason = "file write at " + p;
        return onPathAccess(new PathAccessOperation<T>(CONTENT_WRITE, p, owner, reason));
    }

}
