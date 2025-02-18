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
package com.vivimice.datovn.icue;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import com.vivimice.datovn.spec.CompExecSpec;

public final class IcueSpec implements CompExecSpec {

    private final String name;

    /**
     * Path to the ICUE executable.
     * 
     * Won't be null.
     */
    private final Path executable;

    /**
     * Arguments to pass to the ICUE executable.
     * 
     * Won't be null.
     */
    private final List<String> args;

    /**
     * Parameters of computation.
     * 
     * Won't be null.
     */
    private final List<String> params;

    /**
     * Location of this ICUE specification. Mainly used for logging or diagnostics. 
     * 
     * Won't be null.
     */
    private final String location;

    /**
     * Unique identifier for this ICUE specification.
     * 
     * Won't be null.
     */
    private final String key;

    public IcueSpec(String name, Path executable, List<String> args, List<String> params, String location) {
        if (executable == null || args == null || params == null || location == null) {
            throw new NullPointerException();
        }
        
        this.executable = executable;
        this.args = args;
        this.params = params;
        this.location = location;
        this.key = executable + " " + args.stream().collect(Collectors.joining(" ")) + ";" + params.stream().collect(Collectors.joining(","));
        this.name = name != null ? name : key;
    }

    @Override
    public String getName() {
        return name;
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

    public Path getExecutable() {
        return executable;
    }

    public List<String> getArgs() {
        return args;
    }

    @Override
    public String toString() {
        return getName();
    }

}
