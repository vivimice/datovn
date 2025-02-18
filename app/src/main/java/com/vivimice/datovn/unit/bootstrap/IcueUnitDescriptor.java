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
package com.vivimice.datovn.unit.bootstrap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

@JsonDeserialize(using = IcueUnitDescriptor.JsonDeserializer.class)
public class IcueUnitDescriptor extends UnitDescriptor {

    private String executable;
    private List<String> args;

    public String getExecutable() {
        return executable;
    }

    public void setExecutable(String executable) {
        this.executable = executable;
    }

    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args;
    }

    public static class JsonDeserializer extends StdDeserializer<IcueUnitDescriptor> {

        public JsonDeserializer() {
            super(JsonDeserializer.class);
        }

        @Override
        public IcueUnitDescriptor deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            JsonNode node = p.readValueAsTree();

            IcueUnitDescriptor descriptor = new IcueUnitDescriptor();

            JsonNode commandNode = node.get("command");
            if (commandNode == null) {
                // when "command" field is not present, we raise an exception as this field is required
                throw new JsonMappingException(p, "Missing 'command' field in ICUE unit descriptor.");
            } else if (commandNode.isArray()) {
                // when "command" field is an array, we treat the first element as the executable and the rest as arguments
                descriptor.setExecutable(commandNode.get(0).asText());
                List<String> args = new ArrayList<>();
                for (int i = 1; i < commandNode.size(); i++) {
                    args.add(commandNode.get(i).asText());
                }
                descriptor.setArgs(args);
            } else {
                // when "command" field is a string, we split it by whitespaces and treat the first part as the executable and the rest as arguments
                String[] parts = commandNode.asText().split("\\s+");
                descriptor.setExecutable(parts[0]);
                descriptor.setArgs(List.of(parts).subList(1, parts.length));
            }

            return descriptor;
        }
    }

}
