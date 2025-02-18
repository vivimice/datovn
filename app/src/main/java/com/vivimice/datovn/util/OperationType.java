/*
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

public enum OperationType { 

    /**
     * Read from a path which represents a file.
     */
    CONTENT_READ, 

    /**
     * Write to a path which represents a file.
     */
    CONTENT_WRITE, 

    /**
     * List contents of a directory.
     */
    DIR_LIST,

    /**
     * Create a directory
     */
    DIR_CREATE,

    /**
     * Create a file
     */
    FILE_CREATE,

    /**
     * Check existence of a path
     */
    PATH_CHECK, 
    
    /**
     * Delete a directory or a file.
     */
    PATH_DELETE;

}