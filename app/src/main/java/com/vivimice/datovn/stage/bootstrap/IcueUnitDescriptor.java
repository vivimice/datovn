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

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class IcueUnitDescriptor extends UnitDescriptor {

    private String revision;
    private Object command;

    @JsonIgnore
    private String executable;

    @JsonIgnore
    private List<String> args;

    @Override
    public void afterMapping(ObjectMapper mapper) throws IllegalArgumentException {
        super.afterMapping(mapper);

        if (command == null) {
            throw new IllegalArgumentException("Missing 'command' field in ICUE unit descriptor.");
        }

        List<String> cmd;
        try {
            if (command instanceof Collection) {
                cmd = mapper.convertValue(command, new TypeReference<List<String>>() {});
            } else {
                cmd = List.of(mapper.convertValue(command, String.class).split("\\s+"));
            }
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("'command' field in ICUE unit descriptor must be string or array of strings.");
        }

        if (cmd.isEmpty()) {
            throw new IllegalArgumentException("'command' field in ICUE unit descriptor cannot be empty.");
        }

        executable = cmd.get(0);
        args = List.copyOf(cmd.subList(1, cmd.size()));
    }

    public Object getCommand() {
        return command;
    }

    public void setCommand(Object command) {
        this.command = command;
    }

    @JsonIgnore
    public String getExecutable() {
        return executable;
    }

    @JsonIgnore
    public void setExecutable(String executable) {
        this.executable = executable;
    }

    @JsonIgnore
    public List<String> getArgs() {
        return args;
    }

    @JsonIgnore
    public void setArgs(List<String> args) {
        this.args = args;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

}
