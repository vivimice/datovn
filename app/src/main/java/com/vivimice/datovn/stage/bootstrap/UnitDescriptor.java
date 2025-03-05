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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivimice.datovn.spec.CompExecSpec;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, 
    property = "type", defaultImpl = IcueUnitDescriptor.class, visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = IcueUnitDescriptor.class, name = "icue"),
    @JsonSubTypes.Type(value = FastIcueUnitDescriptor.class, name = "fast-icue"),
})
public class UnitDescriptor {

    private String name;
    private List<String> params;
    @JacksonInject private JsonLocation location;

    private String type;

    private static final Set<String> VALID_TYPES = Stream
        .of(UnitDescriptor.class.getDeclaredAnnotation(JsonSubTypes.class).value())
        .map(type -> type.name())
        .collect(Collectors.toSet());

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

        // Jackson maps unmappable type values with defaultImpl, we need to find out the type
        // is null or malformed
        if (type != null && !VALID_TYPES.contains(type)) {
            throw new IllegalArgumentException("Unknown type: " + type);
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

    public JsonLocation getLocation() {
        return location;
    }

    public void setLocation(JsonLocation location) {
        this.location = location;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
