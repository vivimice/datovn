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
package com.vivimice.datovn.util;

import com.fasterxml.jackson.core.JsonLocation;

public class JsonUtils {

    /**
     * Format a JsonLocation object into a human-readable string (like: `/path/to/foo.json:10:5`)
     * 
     * @param jsonLocation
     * @return
     */
    public static String formatJsonLocation(JsonLocation jsonLocation) {
        return formatJsonLocation(jsonLocation, jsonLocation.sourceDescription());
    }

    /**
     * Format a JsonLocation object into a human-readable string (like: `/path/to/foo.json:10:5`) 
     * with alternative source description. This is useful when the source description of the 
     * JsonLocation object is not available or not meaningful (like: `UNKNOWN_SOURCE`). 
     * 
     * @param jsonLocation
     * @return
     */
    public static String formatJsonLocation(JsonLocation jsonLocation, String sourceDescription) {
        return sourceDescription + ":" + (jsonLocation.getLineNr() + 1) + ":" + jsonLocation.getColumnNr();
    }

}
