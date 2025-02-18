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
package com.vivimice.datovn.error;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * An immutable class that represents a computation error.
 */
public final class CompMessage {

    private final MessageLevel level;
    private final String message;
    private final String stackTrace;
    private final String location;

    public CompMessage(MessageLevel level, String message, String location, Throwable throwable) {
        if (level == null || message == null || location == null) {
            throw new NullPointerException();
        }
        this.level = level;
        this.message = message;
        this.location = location;

        if (throwable == null) {
            throw new NullPointerException();
        }

        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        this.stackTrace = sw.toString();
    }

    public CompMessage(MessageLevel level, String message, String location, String stackTrace) {
        if (level == null || message == null || location == null) {
            throw new NullPointerException();
        }
        this.level = level;
        this.message = message;
        this.stackTrace = stackTrace;
        this.location = location;
    }

    public CompMessage(MessageLevel level, String message, String location) {
        this(level, message, location, (String) null);
    }

    public static CompMessage errorOf(String message, String location) {
        return new CompMessage(MessageLevel.ERROR, message, location);
    }

    /**
     * Get the error level. Won't be null.
     * @return the error level
     */
    public MessageLevel getLevel() {
        return level;
    }

    /**
     * Get the error message. Won't be null.
     * @return the error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get the stack trace. Can be null.
     * @return the stack trace or null
     */
    public String getStackTrace() {
        return stackTrace;
    }
    
    /**
     * Get the location where the error occurred. Won't be null.
     * @return the location
     */
    public String getLocation() {
        return location;
    }
}
