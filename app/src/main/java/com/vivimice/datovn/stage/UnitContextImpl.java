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
import java.util.Map;
import java.util.function.Supplier;

import com.vivimice.datovn.action.MessageLevel;
import com.vivimice.datovn.profiler.UnitProfiler;
import com.vivimice.datovn.spec.CompExecSpec;
import com.vivimice.datovn.unit.UnitContext;

class UnitContextImpl implements UnitContext {

    private final Map<String, StageScopeService> services;
    private final StageContext context;
    private final UnitProfiler profiler;
    private final CompExecSpec spec;

    UnitContextImpl(StageContext context, UnitProfiler profiler, CompExecSpec spec, Map<String, StageScopeService> services) {
        assert context != null;
        assert profiler != null;
        assert spec != null;
        assert services != null;
        
        this.context = context;
        this.profiler = profiler;
        this.spec = spec;
        this.services = services;
    }

    @Override
    public String getStageName() {
        return context.getStageName();
    }

    @Override
    public Path getWorkingDirectory() {
        return context.getStageWorkingDir();
    }

    @Override
    public void logMessage(MessageLevel level, String message, String loc) {
        String location = spec.getName();
        if (loc != null && !loc.isBlank()) {
            location += " (" + loc + ")";
        }

        context.logMessage(level, message, location);
    }

    @Override
    public UnitProfiler getProfiler() {
        return profiler;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends StageScopeService> T getStageService(String key, Supplier<T> factory) {
        return (T) services.computeIfAbsent(key, k -> {
            T service = factory.get();
            service.onInit(context);
            return service;
        });
    }

}