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

import java.util.List;

/**
 * The descriptor POJO for the stage bootstrap unit.
 */
public class StageBootstrapDescriptor {

    private List<UnitDescriptor> units;

    /**
     * "units" section of stage.yml. It contains the list of unit descriptors.
     * 
     * This section is optional. If not specified, means no units.
     * 
     * @return the list of unit descriptors. Might be null.
     */
    public List<UnitDescriptor> getUnits() {
        return units;
    }

    public void setUnits(List<UnitDescriptor> units) {
        this.units = units;
    }

}
