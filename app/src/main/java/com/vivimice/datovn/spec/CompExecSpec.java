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
package com.vivimice.datovn.spec;

import java.util.List;
import java.util.regex.Pattern;

import com.vivimice.datovn.unit.CompUnit;

/**
 * Computation execution specification.
 * 
 * An immutable description of a computation unit ({@link CompUnit}). Any 
 * CompUnit must be created from a CompExecSpec.
 * 
 * A CompExecSpec should be json-serializable.
 */
public interface CompExecSpec {

    public static final Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_.][a-zA-Z0-9_.-]+");

    /**
     * Returns an opaque identifier for this specification.
     * 
     * An opaque identifier is a unique, non-human-readable string that
     * identifies a specification's significant properties. If two specifications
     * shares the same opaque identifier, they are considered equivalent.
     * 
     * Technically, implementations can create this opaque identifier by digesting
     * all significant properties, as well as their class name, of this specification 
     * using a hash function, then encoding the digest using Base64 or Hexadecimal 
     * encoding or similar. 
     */
    String getOpaqueIdentifier();

    /**
     * Returns the name of the computation unit this specification is for.
     * 
     * The name must match {@link #NAME_PATTERN}. Names must be unique across the same stage.
     * 
     * Won't be null.
     */
    String getName();

    /**
     * Returns the location string displayed in error messages.
     * 
     * Won't be null.
     */
    String getLocation();

    /**
     * Returns the arguments to pass to the computation unit upon execution.
     * 
     * Won't be null.
     */
    List<String> getParams();

}