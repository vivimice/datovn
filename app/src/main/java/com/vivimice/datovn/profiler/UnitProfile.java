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

public record UnitProfile (
    String name, 
    String specJson, // might be null if spec serialization failed
    boolean upToDate,
    long scheduleTime,
    long actionsUpdateTime,
    long startTimestamp,
    long endTimestamp,
    int totalActions,
    int totalErrors,
    int totalWarnings,
    int exitCode
) {

    // Constructor for the record
    public UnitProfile(Builder builder) {
        this(
            builder.name,
            builder.specJson,
            builder.upToDate,
            builder.scheduleTime,
            builder.actionsUpdateTime,
            builder.startTimestamp,
            builder.endTimestamp,
            builder.totalActions,
            builder.totalErrors,
            builder.totalWarnings,
            builder.exitCode
        );
    }
    
    // Static method to create a builder instance
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String specJson;
        private boolean upToDate;
        private long scheduleTime;
        private long actionsUpdateTime;
        private long startTimestamp;
        private long endTimestamp;
        private int totalActions;
        private int totalErrors;
        private int totalWarnings;
        private int exitCode;
        
        public Builder() {}

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder specJson(String specJson) {
            this.specJson = specJson;
            return this;
        }

        public Builder upToDate(boolean upToDate) {
            this.upToDate = upToDate;
            return this;
        }

        public Builder scheduleTime(long scheduleTime) {
            this.scheduleTime = scheduleTime;
            return this;
        }

        public Builder actionsUpdateTime(long actionsUpdateTime) {
            this.actionsUpdateTime = actionsUpdateTime;
            return this;
        }

        public Builder startTimestamp(long startTimestamp) {
            this.startTimestamp = startTimestamp;
            return this;
        }

        public Builder endTimestamp(long endTimestamp) {
            this.endTimestamp = endTimestamp;
            return this;
        }

        public Builder totalActions(int totalActions) {
            this.totalActions = totalActions;
            return this;
        }

        public Builder totalErrors(int totalErrors) {
            this.totalErrors = totalErrors;
            return this;
        }

        public Builder totalWarnings(int totalWarnings) {
            this.totalWarnings = totalWarnings;
            return this;
        }

        public Builder exitCode(int exitCode) {
            this.exitCode = exitCode;
            return this;
        }

        public UnitProfile build() {
            return new UnitProfile(this);
        }

    }
}
