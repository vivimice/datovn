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
package com.vivimice.datovn.unit;

import java.util.function.Consumer;

import com.vivimice.datovn.action.CompAction;
import com.vivimice.datovn.action.DirectoryAccessAction;
import com.vivimice.datovn.action.DirectoryAccessMode;
import com.vivimice.datovn.action.ExecAction;
import com.vivimice.datovn.action.ExitAction;
import com.vivimice.datovn.action.FileAccessAction;
import com.vivimice.datovn.action.FileAccessMode;
import com.vivimice.datovn.action.MessageLevel;
import com.vivimice.datovn.action.MessageOutputAction;
import com.vivimice.datovn.spec.CompExecSpec;

/**
 * Recorder which records various actions during computation.
 * 
 * This class is thread-safe, if and only if the consumer passed to the constructor is thread-safe.
 */
public class CompActionRecorder {

    private final Consumer<CompAction.Sketch<?>> consumer;

    public CompActionRecorder(Consumer<CompAction.Sketch<?>> consumer) {
        assert consumer != null;
        this.consumer = consumer;
    }

    public void record(CompAction.Sketch<?> sketch) {
        consumer.accept(sketch);
    }
    
    public void recordCheckFileExists(String path) {
        recordFileAccess(path, FileAccessMode.CHECK_EXISTENCE);
    }

    public void recordReadFile(String path) {
        recordFileAccess(path, FileAccessMode.READ);
    }

    public void recordWriteFile(String path) {
        recordFileAccess(path, FileAccessMode.WRITE);
    }

    public void recordDeleteFile(String path) {
        recordFileAccess(path, FileAccessMode.DELETE);
    }

    public void recordFileAccess(String path, FileAccessMode mode) {
        FileAccessAction.Sketch sketch = new FileAccessAction.Sketch();
        sketch.setPath(path);
        sketch.setMode(mode);
        consumer.accept(sketch);
    }

    public void recordListDirectory(String path) {
        recordDirectoryAccess(path, DirectoryAccessMode.LIST);
    }

    public void recordCreateDirectory(String path) {
        recordDirectoryAccess(path, DirectoryAccessMode.CREATE);
    }

    public void recordDeleteDirectory(String path) {
        recordDirectoryAccess(path, DirectoryAccessMode.DELETE);
    }

    public void recordCheckDirectoryExistence(String path) {
        recordDirectoryAccess(path, DirectoryAccessMode.CHECK_EXISTENCE);
    }

    public void recordDirectoryAccess(String path, DirectoryAccessMode mode) {
        DirectoryAccessAction.Sketch sketch = new DirectoryAccessAction.Sketch();
        sketch.setPath(path);
        sketch.setMode(mode);
        consumer.accept(sketch);
    }

    public void recordMessage(MessageLevel level, String message) {
        MessageOutputAction.Sketch sketch = new MessageOutputAction.Sketch();
        sketch.setLevel(level);
        sketch.setMessage(message);
        consumer.accept(sketch);
    }

    public void recordFatalError(String message) {
        recordMessage(MessageLevel.FATAL, message);
    }

    public void recordError(String message) {
        recordMessage(MessageLevel.ERROR, message);
    }

    public void recordWarning(String message) {
        recordMessage(MessageLevel.WARN, message);
    }

    public void recordInfo(String message) {
        recordMessage(MessageLevel.INFO, message);
    }

    public void recordExit(int exitCode) {
        ExitAction.Sketch sketch = new ExitAction.Sketch();
        sketch.setExitCode(exitCode);
        consumer.accept(sketch);
    }

    public void recordExec(CompExecSpec spec) {
        ExecAction.Sketch sketch = new ExecAction.Sketch();
        sketch.setSpec(spec);
        consumer.accept(sketch);
    }

}
