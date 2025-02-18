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
package com.vivimice.datovn.spec;

import java.util.List;
import java.util.stream.Collectors;

public final class FastIcueSpec implements CompExecSpec {

    private final String name;

    /**
     * Name of the FastICUE daemon
     * 
     * Won't be null.
     */
    private final String daemonName;

    /**
     * Parameters of computation.
     * 
     * Won't be null.
     */
    private final List<String> params;

    /**
     * Location of this FastICUE specification. Mainly used for logging or diagnostics. 
     * 
     * Won't be null.
     */
    private final String location;

    /**
     * Unique identifier for this FastICUE specification.
     * 
     * Won't be null.
     */
    private final String key;

    public FastIcueSpec(String name, String daemonName, List<String> params, String location) {
        if (daemonName == null || params == null || location == null) {
            throw new NullPointerException();
        }
        
        this.daemonName = daemonName;
        this.params = params;
        this.location = location;
        this.key = "@" + daemonName + ";" + params.stream().collect(Collectors.joining(","));
        this.name = name != null ? name : key;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getDaemonName() {
        return daemonName;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public List<String> getParams() {
        return params;
    }

    @Override
    public String getLocation() {
        return location;
    }
    
    @Override
    public String toString() {
        return getName();
    }

}
