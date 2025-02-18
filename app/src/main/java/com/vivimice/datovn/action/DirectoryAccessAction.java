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
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.PatternSyntaxException;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vivimice.datovn.DatovnRuntimeException;

public final class DirectoryAccessAction extends CompAction {

    public static final String DEFAULT_PATTERN = "glob:**/*";

    private static final Logger logger = LoggerFactory.getLogger(DirectoryAccessAction.class);

    private final String path;
    private final String pattern;
    private final String stat;
    private final DirectoryAccessMode mode;

    @JsonCreator
    public DirectoryAccessAction(
        @JsonProperty("path") String path, 
        @JsonProperty("pattern") String pattern, 
        @JsonProperty("mode") DirectoryAccessMode mode,
        @JsonProperty("stat") String stat
    ) {
        assert path != null && !path.isEmpty();
        assert pattern != null && !pattern.isEmpty();
        assert mode != null;
        assert stat != null && !stat.isEmpty();
        
        this.path = path;
        this.pattern = pattern;
        this.mode = mode;
        this.stat = stat;
    }

    public final String getStat() {
        return stat;
    }

    public final String getPath() {
        return path;
    }

    public final String getPattern() {
        return pattern;
    }

    public final DirectoryAccessMode getMode() {
        return mode;
    }

    @Override
    public boolean isUpToDate(ActionPathMappingContext context) {
        Path p = context.resolveFromStore(path); // relative paths (if any) recorded in CompActions are always based on store directory
        String newStat = calculateStat(p, pattern != null ? pattern : DEFAULT_PATTERN);
        boolean upToDate = Objects.equals(stat, newStat);
        if (logger.isTraceEnabled()) {
            if (upToDate) {
                logger.trace("Directory access up-to-date: {}", p);
            } else {
                logger.trace("Directory access not up-to-date: {}, recorded stat: {}, new stat: {}", p, stat, newStat);
            }
        }
        return upToDate;
    }

    @Override
    public CompAction.Sketch<?> toSketch(ActionPathMappingContext context) {
        Sketch sketch = new Sketch();
        sketch.setMode(mode);
        sketch.setPath(context.storeToStage(path));
        sketch.setPattern(Optional.of(pattern));
        return sketch;
    }

    private static String calculateStat(Path p, String pattern) {
        if (!Files.exists(p)) {
            return "stat:not-exists";
        } else if (!Files.isDirectory(p)) {
            return "stat:not-directory";
        }

        PathMatcher matcher;
        try {
            matcher = p.getFileSystem().getPathMatcher(pattern);
        } catch (PatternSyntaxException ex) {
            throw new DatovnRuntimeException("invalid pattern: " + pattern, ex);
        }

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new AssertionError("Can't get directory version", ex);
        }

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(p, entry -> matcher.matches(entry.getFileName()))) {
            AtomicInteger counter = new AtomicInteger();
            StreamSupport
                .stream(ds.spliterator(), false)
                .map(Path::getFileName)
                .sorted()
                .forEach(rel -> {
                    counter.incrementAndGet();
                    md.update(rel.toString().getBytes(StandardCharsets.UTF_8));
                });
            // content: xxxxxxxx (123 files)
            return "namesum:" + HexFormat.of().formatHex(md.digest()) + " (" + counter.get() + ")";
        } catch (IOException ex) {
            return "err:io";
        }
    }

    public static class Sketch extends CompAction.Sketch<DirectoryAccessAction> {

        private String path;
        private Optional<String> pattern;
        private DirectoryAccessMode mode;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public Optional<String> getPattern() {
            return pattern;
        }

        public void setPattern(Optional<String> pattern) {
            this.pattern = pattern;
        }

        public DirectoryAccessMode getMode() {
            return mode;
        }

        public void setMode(DirectoryAccessMode mode) {
            this.mode = mode;
        }
        
        @Override
        public DirectoryAccessAction toAction(ActionPathMappingContext ctx) {
            String pattern = this.pattern.orElse("*");
            Path p = ctx.resolveFromStage(path);
            String stat = calculateStat(p, this.pattern.orElse(DEFAULT_PATTERN));
            return new DirectoryAccessAction(ctx.stageToStore(path), pattern, mode, stat);
        }
    }
}
