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

public class FastIcueConstants {

    public static final String PROTOCOL = "FastICUE/1.0";

    public static final int STATUS_OK = 200;
    public static final int STATUS_ACCEPTED = 202;
    public static final int STATUS_BAD_REQUEST = 400;
    public static final int STATUS_INTERNAL_ERROR = 500;
    public static final int STATUS_SERVICE_UNAVAILABLE = 503;
    public static final int STATUS_VERSION_NOT_SUPPORTED = 505;

    public static final String METHOD_EXEC = "EXEC";
    public static final String METHOD_PING = "PING";

    public static final char FRAME_TYPE_REQUEST = 'Q';
    public static final char FRAME_TYPE_HEADER = 'H';
    public static final char FRAME_TYPE_TERMINATION = 'Z';
    public static final char FRAME_TYPE_RESPONSE_STATUS = 'R';
    public static final char FRAME_TYPE_LINE_DATA = 'L';
    public static final char FRAME_TYPE_BASE64_DATA = 'B';
    
}
