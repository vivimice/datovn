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
package com.vivimice.datovn.action;

import java.io.BufferedReader;
import java.io.IOException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A reader for reading a sequence of CompAction.Sketch<?> objects from a underlying BufferedReader.
 */
public class SketchDocumentReader implements AutoCloseable {

    private static final ObjectMapper mapper = ActionsStore.getActionsMapper();

    private final BufferedReader reader;
    private boolean eof = false;
    private int currentLineNumber = 0;
    private int documentStartLineNumber;

    public SketchDocumentReader(BufferedReader reader) {
        assert reader != null; // precondition check
        this.reader = reader;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    /**
     * Reads the next CompAction.Sketch<?> object from the underlying reader.
     * 
     * @throws IOException if an I/O error occurs while reading from the reader.
     * @throws MalformedActionDocumentException if the content is malformed to represent any valid CompAction.Sketch<?> object.
     * @return the next CompAction.Sketch<?> object from the underlying reader or `null` if end of input is reached.
     */
    public CompAction.Sketch<?> read() throws IOException, MalformedActionDocumentException {
        if (eof) {
            return null;
        }
        
        String buffer = "";
        while (true) {
            String line = reader.readLine();
            if (line != null) {
                currentLineNumber++;
                if (currentLineNumber == 1 || line == "---") {
                    documentStartLineNumber = currentLineNumber;
                }
            }

            if (line == null) {
                eof = true;
                if (buffer != null && !buffer.isBlank()) {
                    return map(buffer, documentStartLineNumber);
                } else {
                    return null;
                }
            } else if (line == "---") {
                // new document
                String source = buffer;
                buffer = line + "\n";
                if (!source.isBlank()) {
                    return map(source, documentStartLineNumber);
                }
            } else {
                buffer += line + "\n";
            }
        }
    }

    private CompAction.Sketch<?> map(String source, int lineNumber) throws MalformedActionDocumentException {
        try {
            return mapper.readValue(source, new TypeReference<CompAction.Sketch<?>>() {});
        } catch (IOException ex) {
            throw new MalformedActionDocumentException("Malformed yaml document begining at line " + lineNumber + ", document: \n" + source, ex);
        }
    }

}
