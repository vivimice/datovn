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

import com.vivimice.datovn.spec.CompExecSpec;

public abstract class AbstractCompUnit<T extends CompExecSpec> implements CompUnit {

    protected final T spec;

    protected AbstractCompUnit(T spec) {
        if (spec == null) {
            throw new NullPointerException();
        }
        this.spec = spec;
    }

    @Override
    public void execute(UnitContext ctx, CompActionRecorder receiver) {
        doCompute(ctx, receiver);
    }

    protected abstract void doCompute(UnitContext ctx, CompActionRecorder receiver);

}
