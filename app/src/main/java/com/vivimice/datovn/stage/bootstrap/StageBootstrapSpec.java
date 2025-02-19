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

import com.vivimice.datovn.spec.CompExecSpec;
import com.vivimice.datovn.util.DigestUtils;

public final class StageBootstrapSpec implements CompExecSpec {

    private static final String OPAQUE_IDENTIFIER = DigestUtils.sha256Hex("bootstrap");
    private final String location;
    
    public StageBootstrapSpec(String location) {
        assert location != null;
        this.location = location;
    }

    @Override
    public String getName() {
        return "bootstrap";
    }

    @Override
    public String getOpaqueIdentifier() {
        return OPAQUE_IDENTIFIER;
    }

    @Override
    public String getLocation() {
        return location;
    }

    @Override
    public List<String> getParams() {
        return List.of();
    }

    @Override
    public String toString() {
        return getName();
    }

}
