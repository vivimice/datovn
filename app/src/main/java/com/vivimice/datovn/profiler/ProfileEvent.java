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
package com.vivimice.datovn.profiler;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public record ProfileEvent(
    /**
     * Number of nanoseconds since the build start
     */
    long clock,

    /**
     * Name of the event.
     */
    String name,
    
    /**
     * Additional data associated with the event. This can be any JSON-serializable object. Won't be null.
     */
    @JsonAnySetter
    @JsonAnyGetter
    Map<String, Object> data

) {}
