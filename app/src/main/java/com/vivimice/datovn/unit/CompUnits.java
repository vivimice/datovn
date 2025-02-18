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

import com.vivimice.datovn.icue.IcueSpec;
import com.vivimice.datovn.icue.IcueUnit;
import com.vivimice.datovn.spec.CompExecSpec;

public class CompUnits {

    /**
     * Creates a computation unit from the given specification.
     * 
     * @param spec The specification of the computation unit. Can't be null.
     * @return The computation unit.
     */
    public static CompUnit create(CompExecSpec spec) {
        assert spec != null;

        if (spec instanceof IcueSpec) {
            return new IcueUnit((IcueSpec) spec);
        }

        throw new IllegalArgumentException("Unknown computation specification: " + spec.getKey());
    }

}
