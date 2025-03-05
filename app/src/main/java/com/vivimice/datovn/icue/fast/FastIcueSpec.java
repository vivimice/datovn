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
package com.vivimice.datovn.icue.fast;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vivimice.datovn.icue.CommandBasedSpec;

public final class FastIcueSpec extends CommandBasedSpec {

    @JsonCreator
    public FastIcueSpec(
        @JsonProperty("name") String name, 
        @JsonProperty("revision") String revision, 
        @JsonProperty("executable") String executable, 
        @JsonProperty("args") List<String> args, 
        @JsonProperty("params") List<String> params
    ) {
        super("fast-icue", name, revision, executable, args, params);
    }

}
