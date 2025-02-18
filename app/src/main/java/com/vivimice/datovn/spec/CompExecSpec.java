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

import com.vivimice.datovn.stage.CompStage;
import com.vivimice.datovn.unit.CompUnit;

/**
 * Computation execution specification.
 * 
 * An immutable description of a computation unit ({@link CompUnit}). Any 
 * CompUnit must be created from a CompExecSpec.
 * 
 * A CompExecSpec is immutable and identified by a key, which is unique 
 * within the scope of the all {@link CompStage}s during the same build.
 * 
 */
public interface CompExecSpec {

    public static final Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_.][a-zA-Z0-9_.-]+");

    /**
     * Returns the unique identifier for the computation unit this specification is for.
     * 
     * Won't be null.
     */
    String getKey();

    /**
     * Returns the name of the computation unit this specification is for.
     * 
     * The name must match {@link #NAME_PATTERN}.
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