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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An action that represents the exit status of a CompUnit.
 */
public final class ExitAction extends CompAction {

    private final int exitCode;

    @JsonCreator
    public ExitAction(@JsonProperty("exitCode") int exitCode) {
        this.exitCode = exitCode;
    }

    public int getExitCode() {
        return exitCode;
    }

    @Override
    public boolean isUpToDate(ActionPathMappingContext context) {
        return true;
    }

    @Override
    public CompAction.Sketch<?> toSketch(ActionPathMappingContext context) {
        Sketch sketch = new Sketch();
        sketch.setExitCode(exitCode);
        return sketch;
    }

    public static class Sketch extends CompAction.Sketch<ExitAction> {

        private int exitCode;

        public int getExitCode() {
            return exitCode;
        }

        public void setExitCode(int exitCode) {
            this.exitCode = exitCode;
        }

        @Override
        public ExitAction toAction(ActionPathMappingContext context) {
            return new ExitAction(exitCode);
        }

    }

}
