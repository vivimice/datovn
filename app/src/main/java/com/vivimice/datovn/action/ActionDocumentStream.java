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
import java.io.Writer;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class ActionDocumentStream {

    private final static ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    // Mapping of document "type" field to action class
    private final static Map<String, Class<? extends CompAction>> actionClasses = Map.of(
        "exit", ExitAction.class,
        "message", MessageOutputAction.class,
        "fileAccess", FileAccessAction.class,
        "directoryAccess", DirectoryAccessAction.class
    );

    // Mapping of document "type" field to sketch class
    private final static Map<String, Class<? extends CompAction.Sketch<?>>> sketchClasses = Map.of(
        "exit", ExitAction.Sketch.class,
        "message", MessageOutputAction.Sketch.class,
        "fileAccess", FileAccessAction.Sketch.class,
        "directoryAccess", DirectoryAccessAction.Sketch.class
    );

    // Mapping of action/sketch class to document "type" field, derived from actionClasses and sketchClasses
    private final static Map<Class<?>, String> actionClassTypeMap = Stream
        .<Map.Entry<String, ? extends Class<?>>>concat(actionClasses.entrySet().stream(), sketchClasses.entrySet().stream())
        .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

    /**
     * Returns an ActionDocumentIterator that reads CompAction.Sketch<?> documents from the given BufferedReader. return each CompAction instance on 'read' call.
     * 
     * If I/O error occurs while reading the document, a DatovnRuntimeException will be thrown.
     * 
     * The returned iterator is not thread-safe.
     * 
     * @param reader the reader to read the CompAction documents from. Cannot be null.
     */
    public static ActionDocumentIterator<CompAction.Sketch<?>> readSketches(BufferedReader reader) {
        return read(reader, (documentMap, lineNumber) -> convertDocument(documentMap, sketchClasses, lineNumber));
    }

    /**
     * Returns an ActionDocumentIterator that reads CompAction documents from the given BufferedReader. return each CompAction instance on 'read' call.
     * 
     * If I/O error occurs while reading the document, a DatovnRuntimeException will be thrown.
     * 
     * The returned iterator is not thread-safe.
     * 
     * @param reader the reader to read the CompAction documents from. Cannot be null.
     */
    public static ActionDocumentIterator<CompAction> readActions(BufferedReader reader) {
        return read(reader, (documentMap, lineNumber) -> convertDocument(documentMap, actionClasses, lineNumber));
    }

    private static <T> T convertDocument(Map<String, Object> documentMap, Map<String, Class<? extends T>> typeMap, int lineNumber) {
        Object typeVal = documentMap.remove("type");
        if (typeVal == null) {
            throw new MalformedActionDocumentException("Action document (starting at line " + lineNumber + ") must have a 'type' field");
        } else if (!(typeVal instanceof String)) {
            throw new MalformedActionDocumentException("Action document (starting at line " + lineNumber + ") 'type' field must be a string");
        }

        String type = (String) typeVal;
        Class<? extends T> clazz = typeMap.get(type);
        if (clazz == null) {
            throw new MalformedActionDocumentException("Action document (starting at line " + lineNumber + ") has unsupported action type: " + type);
        }

        try {
            return yamlMapper.convertValue(documentMap, clazz);
        } catch (IllegalArgumentException ex) {
            throw new MalformedActionDocumentException("Action document (starting at line " + lineNumber + ") with type '" + type + "' is malformed");
        }
    }

    private static <T> ActionDocumentIterator<T> read(BufferedReader reader, BiFunction<Map<String, Object>, Integer, T> mapper) {
        return new ActionDocumentIterator<T>() {
            private boolean eof = false;
            private int currentLineNumber = 0;
            private int documentStartLineNumber;

            @Override
            public T read() throws IOException, MalformedActionDocumentException {
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
                        return map(buffer, documentStartLineNumber);
                    } else if (line == "---") {
                        // new document
                        String source = buffer;
                        buffer = line + "\n";
                        if (!source.isEmpty()) {
                            return map(source, documentStartLineNumber);
                        }
                    } else {
                        buffer += line + "\n";
                    }
                }
            }

            private T map(String document, int lineNumber) {
                try {
                    return mapper.apply(yamlMapper.readValue(document, new TypeReference<Map<String, Object>>() {}), lineNumber);
                } catch (IOException ex) {
                    throw new MalformedActionDocumentException("Malformed yaml document begining at line " + lineNumber, ex);
                }
            }
        };
    }

    /**
     * Writes the given CompAction documents to the given Writer.
     * 
     * @param writer the writer to write the CompAction documents to. Cannot be null.
     * @param actions the CompAction documents to write. Cannot be null.
     * @throws IOException if an I/O error occurs while writing the documents
     */
    public static void writeActions(Writer writer, Iterable<CompAction> actions) throws IOException {
        write(writer, actions);
    }

    /**
     * Writes the given CompAction.Sketch<?> documents to the given Writer.
     * 
     * @param writer the writer to write the CompAction documents to. Cannot be null.
     * @param sketches the CompAction.Sketch<?> documents to write. Cannot be null.
     * @throws IOException if an I/O error occurs while writing the documents
     */
    public static void writeSketches(Writer writer, Iterable<CompAction.Sketch<?>> sketches) throws IOException {
        write(writer, sketches);
    }

    private static <T> void write(Writer writer, Iterable<T> documents) throws IOException {
        for (T document : documents) {
            String type = actionClassTypeMap.get(document.getClass());
            assert type != null;
            Map<String, Object> m = yamlMapper.convertValue(documents, new TypeReference<Map<String, Object>>() {});
            m.put("type", type);
            yamlMapper.writeValue(writer, m);
        }
    }

}
