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
import com.vivimice.datovn.spec.CompExecSpec;

public final class ExecAction extends CompAction {

    private final CompExecSpec spec;

    @JsonCreator
    public ExecAction(@JsonProperty("spec") CompExecSpec spec) {
        assert spec != null;
        this.spec = spec;
    }

    @Override
    public CompAction.Sketch<?> toSketch(ActionPathMappingContext context) {
        Sketch sketch = new Sketch();
        sketch.setSpec(spec);
        return sketch;
    }

    @Override
    public boolean isUpToDate(ActionPathMappingContext context) {
        return true;
    }

    public CompExecSpec getSpec() {
        return spec;
    }

    public static class Sketch extends CompAction.Sketch<ExecAction> {

        private CompExecSpec spec;

        public CompExecSpec getSpec() {
            return spec;
        }

        public void setSpec(CompExecSpec spec) {
            this.spec = spec;
        }

        @Override
        public ExecAction toAction(ActionPathMappingContext context) {
            assert spec != null;
            return new ExecAction(spec);
        }

    }

}
