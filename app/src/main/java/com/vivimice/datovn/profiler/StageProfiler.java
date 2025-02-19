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
package com.vivimice.datovn.profiler;

import java.util.Map;

public class StageProfiler extends AbstractProfiler {

    StageProfiler(ProfilerContext context) {
        super(context);
        setCommonEventData("stageId", context.nextId());
    }

    public UnitProfiler createUnitProfiler() {
        return new UnitProfiler(context);
    }

    public ProfilerCloseable wrapStageRun(String stageName) {
        return wrapEvent("stageRun", Map.of("name", stageName));
    }

}
