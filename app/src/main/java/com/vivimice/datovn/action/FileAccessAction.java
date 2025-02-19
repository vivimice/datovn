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
package com.vivimice.datovn.action;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vivimice.datovn.DatovnRuntimeException;

public final class FileAccessAction extends CompAction {

    private static final Logger logger = LoggerFactory.getLogger(FileAccessAction.class);

    private final String path;
    private final String stat;
    private final FileAccessMode mode;

    @JsonCreator
    public FileAccessAction(
        @JsonProperty("path") String path, 
        @JsonProperty("mode") FileAccessMode mode,
        @JsonProperty("stat") String stat
    ) {
        assert path != null && !path.isEmpty();
        assert stat != null && !stat.isEmpty();
        assert mode != null;

        this.path = path;
        this.stat = stat;
        this.mode = mode;
    }

    @Override
    public boolean isUpToDate(ActionPathMappingContext context) {
        Path p = context.resolveFromStore(path); // relative paths (if any) recorded in CompActions are always based on store directory
        String newStat = calculateStat(p);
        boolean upToDate = Objects.equals(stat, newStat);
        if (logger.isTraceEnabled()) {
            if (upToDate) {
                logger.trace("File access up-to-date: {}", p);
            } else {
                logger.trace("File access not up-to-date: {}, recorded stat: {}, new stat: {}", p, stat, newStat);
            }
        }
        return upToDate;
    }

    public String getPath() {
        return path;
    }

    public String getStat() {
        return stat;
    }

    public FileAccessMode getMode() {
        return mode;
    }

    @Override
    public String toString() {
        return "FileAccessAction [path=" + path + ", stat=" + stat + ", mode=" + mode + "]";
    }

    @Override
    public CompAction.Sketch<?> toSketch(ActionPathMappingContext context) {
        Sketch sketch = new Sketch();
        sketch.setMode(mode);
        sketch.setPath(context.storeToStage(path));
        return sketch;
    }

    private static String calculateStat(Path p) {
        if (!Files.exists(p)) {
            return "stat:not-exists";
        }

        try {
            return "mtime:" + Files.getLastModifiedTime(p).toMillis();
        } catch (IOException ex) {
            throw new DatovnRuntimeException("i/o error while getting file last modified time for file: " + p, ex);
        }
    }

    /**
     * Sketch which is produced by CompUnit.
     */
    public static class Sketch extends CompAction.Sketch<FileAccessAction> {

        private String path;
        private FileAccessMode mode;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public FileAccessMode getMode() {
            return mode;
        }

        public void setMode(FileAccessMode mode) {
            this.mode = mode;
        }

        @Override
        public FileAccessAction toAction(ActionPathMappingContext ctx) {
            Path p = ctx.resolveFromStage(path);
            String stat = calculateStat(p);
            String relPath = ctx.relativizeToStore(p);
            return new FileAccessAction(relPath, mode, stat);
        }

    }

}
