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

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vivimice.datovn.spec.CompExecSpec;
import com.vivimice.datovn.util.DigestUtils;

public final class IcueSpec implements CompExecSpec {

    private final String name;

    /**
     * Path to the ICUE executable.
     * 
     * Won't be null.
     */
    private final String executable;

    /**
     * An additional revision string, used to alter opaque identifier manually in spec.
     * 
     * Might be null.
     */
    private final String revision;

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
     * Opaque identifier for this spec.
     */
    private final String opaqueIdentifier;

    @JsonCreator
    public IcueSpec(
        @JsonProperty("name") String name, 
        @JsonProperty("revision") String revision, 
        @JsonProperty("executable") String executable, 
        @JsonProperty("args") List<String> args, 
        @JsonProperty("params") List<String> params
    ) {
        if (executable == null || args == null || params == null) {
            throw new NullPointerException();
        }
        
        this.executable = executable;
        this.args = args;
        this.params = params;
        this.revision = revision;

        String keyProperties = "icue:" + executable + ":" + args.stream().collect(Collectors.joining(" ")) + ";" + params.stream().collect(Collectors.joining(","));
        if (revision != null) {
            keyProperties += "@rev=" + revision;
        }
        this.opaqueIdentifier = DigestUtils.sha256Hex(keyProperties);

        if (name == null) {
            name = "unnamed_" + opaqueIdentifier.substring(0, 8) + "";
        }
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getRevision() {
        return revision;
    }

    @Override
    public String getOpaqueIdentifier() {
        return opaqueIdentifier;
    }

    @Override
    public List<String> getParams() {
        return params;
    }

    public String getExecutable() {
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
