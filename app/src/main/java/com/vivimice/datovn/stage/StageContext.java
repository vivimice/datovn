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
import java.util.concurrent.Executor;

import org.slf4j.MDC;
import org.slf4j.MDC.MDCCloseable;

import com.vivimice.datovn.action.ActionsStore;
import com.vivimice.datovn.action.MessageLevel;
import com.vivimice.datovn.profiler.StageProfiler;

/**
 * The context object that provides information and services for a specific computation stage.
 * 
 * Note: implementation of this interface should be thread-safe.
 */
public interface StageContext {

    /**
     * Get the name of the current computation stage.
     * 
     * The name should be unique within a computation build, and should be a single valid file name or directory name.
     */
    String getStageName();

    /**
     * Get the thread pool executor to run CompUnit
     */
    Executor getCompUnitThreadPool();

    /**
     * Get the store which is able to open streams to store CompUnit's actions.
     */
    ActionsStore getActionsStore();

    /**
     * Get the working directory of the current stage.
     */
    Path getStageWorkingDir();

    /**
     * Get the profiler for this stage.
     */
    StageProfiler getProfiler();

    /**
     * Log a message with specified level.
     */
    void logMessage(MessageLevel level, String message, String location);

    /**
     * Log the progress of the current computation stage. The progress should be in range [0, 1].
     * @param progress
     * @param description
     */
    void logProgress(double progress, String message);

    default MDCCloseable putMdcClosable() {
        return MDC.putCloseable("stage", getStageName());
    }

}
