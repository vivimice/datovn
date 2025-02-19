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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for ActionDocumentStream class.
 */
public class ActionDocumentStreamTest {

    @Mock
    private BufferedReader reader;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testReadEmptyInputs() throws IOException {
        // Arrange
        when(reader.readLine()).thenReturn((String) null);
        // Act
        ActionDocumentIterator<CompAction.Sketch<?>> iterator = ActionDocumentStream.readSketches(reader);
        // Assert
        assertNull(iterator.read());
    }

    @Test
    public void testReadSketches() throws IOException {
        // Arrange
        when(reader.readLine()).thenReturn(
            "---",
            "type: message", 
            "level: INFO", 
            "message: Test message", 
            (String) null);

        // Act
        ActionDocumentIterator<CompAction.Sketch<?>> iterator = ActionDocumentStream.readSketches(reader);

        // Assert
        CompAction.Sketch<?> sketch = iterator.read();
        assertNotNull(sketch);
        assertNull(iterator.read());
    }

    @Test
    public void testReadActions() throws IOException {
        // Arrange
        when(reader.readLine()).thenReturn(
            "---",
            "type: message", 
            "level: INFO", 
            "message: Test message", 
            (String) null);

        // Act
        ActionDocumentIterator<CompAction> iterator = ActionDocumentStream.readActions(reader);

        // Assert
        CompAction action = iterator.read();
        assertNotNull(action);
        assertNull(iterator.read());
    }

    @Test
    public void testReadSketchesWithMalformedDocument() throws IOException {
        // Arrange
        when(reader.readLine()).thenReturn(
            "type: invalidType", 
            "level: INFO", 
            "message: Test message", 
            (String) null);

        // Act & Assert
        ActionDocumentIterator<CompAction.Sketch<?>> iterator = ActionDocumentStream.readSketches(reader);
        assertThrows(MalformedActionDocumentException.class, iterator::read);
    }

    @Test
    public void testReadActionsWithMalformedDocument() throws IOException {
        // Arrange
        when(reader.readLine()).thenReturn(
            "type: invalidType", 
            "level: INFO", 
            "message: Test message", 
            (String) null);

        // Act & Assert
        ActionDocumentIterator<CompAction> iterator = ActionDocumentStream.readActions(reader);
        assertThrows(MalformedActionDocumentException.class, iterator::read);
    }

    @Test
    public void testReadSketchesWithIOException() throws IOException {
        // Arrange
        when(reader.readLine()).thenThrow(new IOException("Simulated IO error"));

        // Act & Assert
        ActionDocumentIterator<CompAction.Sketch<?>> iterator = ActionDocumentStream.readSketches(reader);
        assertThrows(IOException.class, iterator::read);
    }

    @Test
    public void testReadActionsWithIOException() throws IOException {
        // Arrange
        when(reader.readLine()).thenThrow(new IOException("Simulated IO error"));

        // Act & Assert
        ActionDocumentIterator<CompAction> iterator = ActionDocumentStream.readActions(reader);
        assertThrows(IOException.class, iterator::read);
    }

    @Test
    public void testReadSketchesWithMultipleDocuments() throws IOException {
        // Arrange
        // Simulate reading multiple YAML documents separated by "---"
        when(reader.readLine()).thenReturn(
            "type: message",
            "level: INFO",
            "message: Test message 1",
            "---", 
            "type: message", 
            "level: ERROR",
            "message: Test message 2",
            "---", 
            "type: message",
            "level: WARN",
            "message: Test message 3",
            (String) null
        );

        // Act
        ActionDocumentIterator<CompAction.Sketch<?>> iterator = ActionDocumentStream.readSketches(reader);  

        // Assert
        CompAction.Sketch<?> sketch1 = iterator.read();
        assertNotNull(sketch1);
        assertEquals("INFO", ((MessageOutputAction.Sketch) sketch1).getLevel().toString());
        assertEquals("Test message 1", ((MessageOutputAction.Sketch) sketch1).getMessage());

        CompAction.Sketch<?> sketch2 = iterator.read();
        assertNotNull(sketch2);
        assertEquals("ERROR", ((MessageOutputAction.Sketch) sketch2).getLevel().toString());
        assertEquals("Test message 2", ((MessageOutputAction.Sketch) sketch2).getMessage());

        CompAction.Sketch<?> sketch3 = iterator.read();
        assertNotNull(sketch3);
        assertEquals("WARN", ((MessageOutputAction.Sketch) sketch3).getLevel().toString());
        assertEquals("Test message 3", ((MessageOutputAction.Sketch) sketch3).getMessage());

        assertNull(iterator.read());
    }

    @Test
    public void testReadActionsWithMultipleDocuments() throws IOException {
        // Arrange
        // Simulate reading multiple YAML documents separated by "---"
        when(reader.readLine()).thenReturn(
            "type: message",
            "level: INFO",
            "message: Test message 1",
            "---", 
            "type: message",
            "level: ERROR",
            "message: Test message 2",
            "---", 
            "type: message",
            "level: WARN",
            "message: Test message 3",
            (String) null
        );

        // Act
        ActionDocumentIterator<CompAction> iterator = ActionDocumentStream.readActions(reader);

        // Assert
        CompAction action1 = iterator.read();
        assertNotNull(action1);
        assertEquals("INFO", ((MessageOutputAction) action1).getLevel().toString());
        assertEquals("Test message 1", ((MessageOutputAction) action1).getMessage());

        CompAction action2 = iterator.read();
        assertNotNull(action2);
        assertEquals("ERROR", ((MessageOutputAction) action2).getLevel().toString());
        assertEquals("Test message 2", ((MessageOutputAction) action2).getMessage());

        CompAction action3 = iterator.read();
        assertNotNull(action3);
        assertEquals("WARN", ((MessageOutputAction) action3).getLevel().toString());
        assertEquals("Test message 3", ((MessageOutputAction) action3).getMessage());

        assertNull(iterator.read());
    }

    @Test
    public void testReadSketchesWithMalformedDocumentAndCorrectLineIndex() throws IOException {
        // Arrange
        // Simulate reading a valid YAML document followed by a malformed one
        when(reader.readLine()).thenReturn(
            // well-formed document
            "type: message",
            "level: INFO",
            "message: Test message 1", 
            // Malformed document
            "---", 
            "type: invalidType",
            "level: INFO",
            "message: Test message 2",
            (String) null
        );

        // Act
        ActionDocumentIterator<CompAction.Sketch<?>> iterator = ActionDocumentStream.readSketches(reader);

        // Assert
        CompAction.Sketch<?> sketch1 = iterator.read();
        assertNotNull(sketch1);
        assertEquals("INFO", ((MessageOutputAction.Sketch) sketch1).getLevel().toString());
        assertEquals("Test message 1", ((MessageOutputAction.Sketch) sketch1).getMessage());

        // The second document is malformed, so the next call should throw an exception
        MalformedActionDocumentException exception = assertThrows(MalformedActionDocumentException.class, iterator::read);
        assertTrue(exception.getMessage().contains("starting at line 4")); // Line 3 is where the malformed document starts
    }

    @Test
    public void testReadActionsWithMalformedDocumentAndCorrectLineIndex() throws IOException {
        // Arrange
        // Simulate reading a valid YAML document followed by a malformed one
        when(reader.readLine()).thenReturn(
            "type: message", 
            "level: INFO",
            "message: Test message 1",
            // Malformed document
            "---", 
            "type: invalidType",
            "level: INFO",
            "message: Test message 2",
            (String) null
        );

        // Act
        ActionDocumentIterator<CompAction> iterator = ActionDocumentStream.readActions(reader);

        // Assert
        CompAction action1 = iterator.read();
        assertNotNull(action1);
        assertEquals("INFO", ((MessageOutputAction) action1).getLevel().toString());
        assertEquals("Test message 1", ((MessageOutputAction) action1).getMessage());

        // The second document is malformed, so the next call should throw an exception
        MalformedActionDocumentException exception = assertThrows(MalformedActionDocumentException.class, iterator::read);
        assertTrue(exception.getMessage().contains("starting at line 4")); // Line 3 is where the malformed document starts
    }

}