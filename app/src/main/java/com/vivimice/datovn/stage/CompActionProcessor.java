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
package com.vivimice.datovn.stage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.vivimice.datovn.action.CompAction;
import com.vivimice.datovn.action.DirectoryAccessAction;
import com.vivimice.datovn.action.ExecAction;
import com.vivimice.datovn.action.ExitAction;
import com.vivimice.datovn.action.FileAccessAction;
import com.vivimice.datovn.action.MessageLevel;
import com.vivimice.datovn.action.MessageOutputAction;
import com.vivimice.datovn.spec.CompExecSpec;
import com.vivimice.datovn.unit.CompActionRecorder;
import com.vivimice.datovn.unit.UnitContext;
import com.vivimice.datovn.util.OffendingPathAccessChecker;
import com.vivimice.datovn.util.PathAccessOperation;

class CompActionProcessor implements Consumer<CompAction.Sketch<?>> {

    final List<CompAction.Sketch<?>> recordedSketches = new LinkedList<>();
    private final List<CompExecSpec> invocations = new ArrayList<>();
    private final StageContext stageContext;
    private final UnitContext execContext;
    private final OffendingPathAccessChecker<String> pathAccessChecker;
    private final CompExecSpec spec;
    final List<ProcessingError> processingErrors = new ArrayList<>();

    private boolean hasFatalError = false;
    private Optional<Integer> explicitExitCode = Optional.empty();
    private boolean offendingPathAccessReported = false;

    CompActionProcessor(StageContext stageContext, UnitContext execContext, OffendingPathAccessChecker<String> pathAccessChecker, CompExecSpec spec) {
        assert stageContext != null;
        assert execContext != null;
        assert pathAccessChecker != null;
        assert spec != null;

        this.pathAccessChecker = pathAccessChecker;
        this.stageContext = stageContext;
        this.execContext = execContext;
        this.spec = spec;
    }

    @Override
    public synchronized void accept(CompAction.Sketch<?> sketch) {
        recordedSketches.add(sketch);

        switch (sketch) {
            case ExecAction.Sketch execSketch:
                // we record exec sketches for subsequent execution.
                invocations.add(execSketch.getSpec());
                break;
                
            case FileAccessAction.Sketch fileAccessSketch:
                // we check offending path access against operations from other compunits
                checkPathAccess(fileAccessSketch);
                break;

            case DirectoryAccessAction.Sketch directoryAccessSketch:
                // we check offending path access against operations from other compunits
                checkPathAccess(directoryAccessSketch);
                break;

            case ExitAction.Sketch exitSketch:
                if (explicitExitCode.isPresent()) {
                    reportProcessingError("Explicit exit code already set. Cannot override with another one.", null);
                }
                explicitExitCode = Optional.of(exitSketch.getExitCode());
                if (hasFatalError && exitSketch.getExitCode() == 0) {
                    reportProcessingError("Exit code cannot be zero while there are fatal errors reported.", null);
                }
                break;

            case MessageOutputAction.Sketch messageSketch:
                MessageLevel level = messageSketch.getLevel();
                String message = messageSketch.getMessage();
                String location = messageSketch.getLocation();
                // we record there was a fatal error
                hasFatalError |= (level == MessageLevel.FATAL);
                // we direct all messages to execution context
                execContext.logMessage(level, message, location);
                execContext.getProfiler().onMessage(level, message);
                break;

            default:
               break;
        }
    }

    public List<CompExecSpec> getInvocations() {
        return invocations;
    }

    private void checkPathAccess(FileAccessAction.Sketch sketch) {
        if (offendingPathAccessReported) {
            return;
        }

        checkPathAccess(sketch.getPath(), switch (sketch.getMode()) {
            case CREATE -> pathAccessChecker::onFileCreation;
            case DELETE -> pathAccessChecker::onPathRemoval;
            case READ -> pathAccessChecker::onFileRead;
            case WRITE -> pathAccessChecker::onFileWrite;
            case CHECK_EXISTENCE -> pathAccessChecker::onCheckExistence;
            // should never happen
            default -> (path, owner) -> { 
                throw new AssertionError(); 
            };
        }, "file access with mode '" + sketch.getMode() + "'");
    }

    private void checkPathAccess(DirectoryAccessAction.Sketch sketch) {
        if (offendingPathAccessReported) {
            return;
        }

        checkPathAccess(sketch.getPath(), switch (sketch.getMode()) {
            case CREATE -> pathAccessChecker::onDirectoryCreation;
            case DELETE -> pathAccessChecker::onPathRemoval;
            case LIST -> pathAccessChecker::onDirectoryListing;
            case CHECK_EXISTENCE -> pathAccessChecker::onCheckExistence;
            // should never happen
            default -> (path, owner) -> { 
                throw new AssertionError(); 
            };
        }, "directory access with mode '" + sketch.getMode() + "'");
    }

    private void checkPathAccess(String path, BiFunction<Path, String, PathAccessOperation<String>> checker, String description) {
        Path p = stageContext.getStageWorkingDir().resolve(path).normalize().toAbsolutePath();

        PathAccessOperation<String> offendingOperation;
        synchronized (pathAccessChecker) { // checker is backed by OffendingPathAccessChecker, which is not thread-safe
            offendingOperation = checker.apply(p, spec.getName());
        }

        if (offendingOperation != null) {
            CompActionRecorder reporter = new CompActionRecorder(this);
            reporter.recordFatalError("Our " + description + " at path '" + p + "' offends operation from '" + offendingOperation.owner() + "': " + offendingOperation.reason());
            offendingPathAccessReported = true;
        }
    }

    private void reportProcessingError(String message, String location) {
        processingErrors.add(new ProcessingError(message, location));
    }

}