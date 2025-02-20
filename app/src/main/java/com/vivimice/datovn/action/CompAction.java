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
package com.vivimice.datovn.action;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * A CompAction records a single side-effect that is performed by a CompUnit.
 * 
 * CompAction is also safe for serialization and deserialization.
 * 
 * Subclass of CompAction should be immutable.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ExecAction.class, name = "exec"),
    @JsonSubTypes.Type(value = ExitAction.class, name = "exit"),
    @JsonSubTypes.Type(value = FileAccessAction.class, name = "fileAccess"),
    @JsonSubTypes.Type(value = DirectoryAccessAction.class, name = "directoryAccess"),
    @JsonSubTypes.Type(value = MessageOutputAction.class, name = "messageOutput"),
})
public abstract class CompAction {

    /**
     * Convert this action into a sketch, removing any state recorded.
     */
    abstract public Sketch<?> toSketch(ActionPathMappingContext context);

    /**
     * Check the state record alongside with this action is up-to-date. 
     */
    abstract public boolean isUpToDate(ActionPathMappingContext context);

    /**
     * Sketch is used for deserialize actions produced by CompUnit.
     * 
     * These deserialized actions are different from real CompAction, they don't record
     * state related to the action.
     * 
     * @param <T> Type of the corresponding CompAction.
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = ExecAction.Sketch.class, name = "exec"),
        @JsonSubTypes.Type(value = ExitAction.Sketch.class, name = "exit"),
        @JsonSubTypes.Type(value = FileAccessAction.Sketch.class, name = "fileAccess"),
        @JsonSubTypes.Type(value = DirectoryAccessAction.Sketch.class, name = "directoryAccess"),
        @JsonSubTypes.Type(value = MessageOutputAction.Sketch.class, name = "messageOutput"),
    })
    public static abstract class Sketch<T extends CompAction> {

        public abstract T toAction(ActionPathMappingContext context);

    }

}
