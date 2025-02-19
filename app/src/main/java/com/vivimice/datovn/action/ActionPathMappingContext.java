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

import java.nio.file.Path;

/**
 * A context for populating a CompAction.Sketch produced by CompUnit into a full CompAction.
 */
public class ActionPathMappingContext {

    private final Path stageDirectory;
    private final Path storeDirectory;

    public ActionPathMappingContext(Path stageDirectory, Path storeDirectory) {
        assert stageDirectory != null;
        assert storeDirectory != null;

        this.stageDirectory = stageDirectory;
        this.storeDirectory = storeDirectory;
    }

    public String stageToStore(String stagePath) {
        return storeDirectory.relativize(stageDirectory.resolve(stagePath)).normalize().toString();
    }

    public String storeToStage(String stagePath) {
        return stageDirectory.relativize(storeDirectory.resolve(stagePath)).normalize().toString();
    }

    /**
     * Resolve the given stage-relative path string into a java.nio.file.Path
     */
    public Path resolveFromStage(String stagePath) {
        return stageDirectory.resolve(stagePath).normalize();
    }

    /**
     * Resolve the given store-relative path string into a java.nio.file.Path
     */
    public Path resolveFromStore(String storePath) {
        return storeDirectory.resolve(storePath).normalize();
    }

    /**
     * Relativize the given path to the stage root.
     */
    public String relativizeToStage(Path path) {
        return stageDirectory.relativize(path).normalize().toString();
    }

    /**
     * Relativize the given path to the action store root.
     */
    public String relativizeToStore(Path path) {
        return storeDirectory.relativize(path).normalize().toString();
    }


}
