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
package com.vivimice.datovn.util;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Utility class that injects JsonLocation information into JSON objects.
 * 
 * Inspired by: 
 * - https://github.com/FasterXML/jackson-databind/issues/3869
 * - https://github.com/liblit/jackson-source-location-injection/blob/mainline/src/main/java/SourceLocationInjector.java
 * 
 * Limitations: 
 * - Might be wrong while mapped class is constructed by @JsonCreator
 */
public class InjectableJsonLocationValues extends InjectableValues {

    @Override
    public Object findInjectableValue(Object valueId, DeserializationContext ctxt, BeanProperty forProperty,
            Object beanInstance) throws JsonMappingException {
        JavaType forPropertyType = forProperty.getType();
        if (!forPropertyType.hasRawClass(JsonLocation.class)) {
            throw JsonMappingException.from(ctxt, "Cannot inject value of type %s.".formatted(forPropertyType));
        }

        JsonParser jp = ctxt.getParser();
        return jp.getParsingContext().startLocation(jp.currentTokenLocation().contentReference());
    }

}
