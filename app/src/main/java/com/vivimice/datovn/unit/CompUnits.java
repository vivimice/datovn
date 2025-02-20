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

import com.vivimice.datovn.DatovnRuntimeException;
import com.vivimice.datovn.icue.IcueSpec;
import com.vivimice.datovn.icue.IcueUnit;
import com.vivimice.datovn.spec.CompExecSpec;
import com.vivimice.datovn.stage.bootstrap.IcueUnitDescriptor;
import com.vivimice.datovn.stage.bootstrap.StageBootstrapCompUnit;
import com.vivimice.datovn.stage.bootstrap.StageBootstrapSpec;
import com.vivimice.datovn.stage.bootstrap.UnitDescriptor;

public class CompUnits {

    /**
     * Creates a computation unit from the given specification.
     * 
     * @param spec The specification of the computation unit. Can't be null.
     * @return The computation unit.
     */
    public static CompUnit create(CompExecSpec spec) {
        return switch (spec) {
            case IcueSpec icueSpec -> new IcueUnit(icueSpec);
            case StageBootstrapSpec stageBootstrapSpec -> new StageBootstrapCompUnit(stageBootstrapSpec);
            case null -> throw new NullPointerException("specification can't be null");
            default -> throw new DatovnRuntimeException("Unknown computation specification: " + spec.getName());
        };
    }

    /**
     * Factory method to determine the appropriate descriptor class based on the given type.
     * 
     * @param type "type" field in stage.yml's unit section.
     * @return The descriptor class. Null if the type is unknown.
     */
    public static Class<? extends UnitDescriptor> lookupDescriptorClass(String type) {
        return switch (type) {
            case "icue" -> IcueUnitDescriptor.class;
            case null -> IcueUnitDescriptor.class; // default as ICUE units
            default -> null;
        };
    }

    /**
     * Create a computation execution specification from the given descriptor.
     * 
     * @param descriptor The descriptor of the computation unit defined by stage.yml. Can't be null.
     * @return The computation execution specification. Null if the descriptor type is unknown.
     */
    public static CompExecSpec createSpec(UnitDescriptor descriptor) {
        assert descriptor != null;
        return switch (descriptor) {
            case IcueUnitDescriptor unit -> new IcueSpec(
                unit.getName(), 
                unit.getRevision(),
                unit.getExecutable(),
                unit.getArgs(), 
                unit.getParams(), 
                unit.getLocation().toString()
            );
            default -> null;
        };
    }

}
