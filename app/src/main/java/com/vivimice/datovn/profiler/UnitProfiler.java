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

import com.vivimice.datovn.action.MessageLevel;
import com.vivimice.datovn.spec.CompExecSpec;

/**
 * Profiler for CompUnit.
 *
 * As for all profilers under this package, this class is thread-safe.
 */
public class UnitProfiler extends AbstractProfiler {

    UnitProfiler(ProfilerContext context) {
        super(context);
        setCommonEventData("unitId", context.nextId());
    }

    public void onSchedule(CompExecSpec spec) {
        emitEvent("schedule", Map.of("spec", spec));
    }

    public ProfilerCloseable wrapExecution() {
        return wrapEvent("execution");
    }

    public ProfilerCloseable wrapLoadSketches() {
        return wrapEvent("loadSketches");
    }

    public ProfilerCloseable wrapUnitRun() {
        return wrapEvent("run");
    }

    public void onMessage(MessageLevel level, String message) {
        emitEvent("message", Map.of("level", level, "message", message));
    }

    public ProfilerCloseable wrapWriteSketches() {
        return wrapEvent("writeSketches");
    }

}
