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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vivimice.datovn.error.MessageLevel;

public final class MessageOutputAction extends CompAction {

    /**
     * Message level. Won't be null.
     */
    private final MessageLevel level;

    /**
     * Message. Won't be null.
     */
    private final String message;

    /**
     * Location. Might be null.
     */
    private final String location;

    @JsonCreator
    public MessageOutputAction(
        @JsonProperty("level") MessageLevel level,
        @JsonProperty("message") String message,
        @JsonProperty("location") String location
    ) {
        assert level != null;
        assert message != null;

        this.level = level;
        this.message = message;
        this.location = location;
    }

    public final MessageLevel getLevel() {
        return level;
    }

    public final String getMessage() {
        return message;
    }

    public final String getLocation() {
        return location;
    }

    @Override
    public boolean isUpToDate(ActionPathMappingContext context) {
        return true;
    }

    @Override
    public CompAction.Sketch<?> toSketch(ActionPathMappingContext context) {
        Sketch sketch = new Sketch();
        sketch.setLevel(level);
        sketch.setMessage(message);
        sketch.setLocation(location);
        return sketch;
    }

    public static class Sketch extends CompAction.Sketch<MessageOutputAction> {

        private MessageLevel level;
        private String message;
        private String location;

        public MessageLevel getLevel() {
            return level;
        }

        public void setLevel(MessageLevel level) {
            this.level = level;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        @Override
        public MessageOutputAction toAction(ActionPathMappingContext context) {
            assert level != null;
            assert message != null;
            return new MessageOutputAction(level, message, location);
        }

    }

}
