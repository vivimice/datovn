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
package com.vivimice.datovn.stage.bootstrap;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivimice.datovn.spec.CompExecSpec;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, 
    property = "type", defaultImpl = IcueUnitDescriptor.class
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = IcueUnitDescriptor.class, name = "icue"),
})
public class UnitDescriptor {

    private String name;
    private List<String> params;

    public void afterMapping(ObjectMapper mapper) throws IllegalArgumentException {
        if (params == null) {
            params = List.of();
        }

        if (name == null) {
            throw new IllegalArgumentException("Missing 'name' field of a exec spec.");
        }

        if (!CompExecSpec.NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Illegal exec spec name: " + name);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getParams() {
        return params;
    }

    public void setParams(List<String> params) {
        this.params = params;
    }

}
