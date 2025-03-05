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
package com.vivimice.datovn.icue.fast;

import java.util.LinkedHashMap;
import java.util.Map;

public class FastIcueRequest {

    private final String method;
    private Map<String, String> headers = new LinkedHashMap<>();

    public FastIcueRequest(String method) {
        assert method != null && !method.isEmpty() : "Method cannot be null or empty";
        this.method = method;
    }

    public String getMethod() {
        return method;
    }

    public void setHeader(String header, String value) {
        assert header != null && !header.isEmpty() : "Header cannot be null or empty";
        assert value != null : "Value cannot be null";
        headers.put(header, value);
    }

    public void removeHeader(String header) {
        headers.remove(header);
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(method).append(System.lineSeparator());
        headers.forEach((name, value) -> {
            sb.repeat(" ", 4).append(name).append(": ").append(value).append(System.lineSeparator());
        });
        return sb.toString();
    }

}
