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

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.NullNode;
import com.vivimice.datovn.spec.CompExecSpec;
import com.vivimice.datovn.unit.CompUnits;

@JsonDeserialize(using = UnitDescriptor.JsonDeserializer.class)
public class UnitDescriptor {

    private String name;
    private JsonLocation location;
    private List<String> params;

    @JsonIgnore
    public JsonLocation getLocation() {
        return location;
    }

    @JsonIgnore
    public void setLocation(JsonLocation location) {
        this.location = location;
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

    public static class JsonDeserializer extends StdDeserializer<UnitDescriptor> {

        public JsonDeserializer() {
            super(JsonDeserializer.class);
        }

        @Override
        public UnitDescriptor deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            JsonLocation location = p.currentLocation();
            JsonNode node = p.getCodec().readTree(p);
            String type;

            JsonNode typeNode = node.get("type");
            if (typeNode == null || typeNode instanceof NullNode) {
                type = null;
            } else {
                type = typeNode.asText();
            }

            Class<? extends UnitDescriptor> descriptorClass = CompUnits.lookupDescriptorClass(type);
            if (descriptorClass == null) {
                throw new JsonMappingException(p, "Unknown unit type: " + type);
            }
            UnitDescriptor descriptor = p.getCodec().treeToValue(node, descriptorClass);

            // set name
            JsonNode nameNode = node.get("name");
            if (nameNode != null && !(nameNode instanceof NullNode)) {
                String name = nameNode.asText();
                if (!CompExecSpec.NAME_PATTERN.matcher(name).matches()) {
                    throw new JsonMappingException(p, "Invalid unit name: " + name);
                }
                descriptor.setName(name);
            }

            // set location
            descriptor.setLocation(location);

            // set default values
            if (descriptor.getParams() == null) {
                descriptor.setParams(List.of());
            }

            return descriptor;
        }
    }
}
