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

import java.nio.file.Path;
import java.util.function.Supplier;

import com.vivimice.datovn.action.MessageLevel;
import com.vivimice.datovn.profiler.UnitProfiler;
import com.vivimice.datovn.stage.StageScopeService;

/**
 * Execution context of a CompUnit.
 */
public interface UnitContext {

    /**
     * Get the name of current CompUnit's execution stage.
     */
    String getStageName();

    /**
     * Get the working directory of current CompUnit's execution.
     */
    Path getWorkingDirectory();

    /**
     * Log a message with specified level.
     */
    void logMessage(MessageLevel level, String message, String location);

    /**
     * Get the profiler for current CompUnit.
     */
    UnitProfiler getProfiler();

    /**
     * Get a stage scope service. If not exists, create one using the supplier.
     */
    <T extends StageScopeService> T getStageService(String key, Supplier<T> factory);

}
