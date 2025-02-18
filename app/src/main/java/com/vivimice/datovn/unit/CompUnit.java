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
package com.vivimice.datovn.unit;

public interface CompUnit {

    /**
     * Executes the computation and report the actions as CompAction.Sketch<?> to the given receiver.
     * 
     * @param spec computation specification. Won't be <code>null</code>.
     * @param actionsOutput output stream to write computation actions to. Won't be <code>null</code>.
     */
    void execute(UnitContext context, CompActionRecorder receiver);

}
