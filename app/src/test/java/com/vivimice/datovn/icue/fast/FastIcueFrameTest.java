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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class FastIcueFrameTest {

    @Test
    public void testReadSimpleQFrame() throws Exception {
        String input;
        FastIcueFrameInputStream in;
        FastIcueFrame frame;
        
        input = "01 Q | PING FastICUE/1.0\r\n";
        in = new FastIcueFrameInputStream(
            new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        frame = in.readFrame();
        assertEquals(0x1, frame.invocationId());
        assertEquals('Q', frame.type());
        assertEquals("PING FastICUE/1.0", frame.data());

        input = "01F3 Q | PING FastICUE/1.0\r\n";
        in = new FastIcueFrameInputStream(
            new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        frame = in.readFrame();
        assertEquals(0x1f3, frame.invocationId());
        assertEquals('Q', frame.type());
        assertEquals("PING FastICUE/1.0", frame.data());
    }

    @Test
    public void testPrematureEndedFrame() throws Exception {
        String input = "02 D |";
        FastIcueFrameInputStream in = new FastIcueFrameInputStream(
            new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        
        MalformedFrameException ex = assertThrows(
            MalformedFrameException.class, in::readFrame);
        assertTrue(ex.getMessage().contains("incomplete frame"));
    }

    @Test
    public void testMissingTerminatorThrowsException() {
        String input = "03 Q |";
        FastIcueFrameInputStream in = new FastIcueFrameInputStream(
            new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        
        assertThrows(MalformedFrameException.class, in::readFrame);
    }

    @Test
    public void testInvalidInvocationId() throws Exception {
        String input;
        FastIcueFrameInputStream in;

        input = "ZZ Q | data\r\n";
        in = new FastIcueFrameInputStream(
            new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        assertThrows(MalformedFrameException.class, in::readFrame);

        input = "FFFFFFFFFF Q | data\r\n";
        in = new FastIcueFrameInputStream(
            new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        assertThrows(MalformedFrameException.class, in::readFrame);

        input = "80000000 Q | data\r\n";
        in = new FastIcueFrameInputStream(
            new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        assertThrows(MalformedFrameException.class, in::readFrame);
    }

    @Test
    public void testErrorContextContainsRecentBytes() throws IOException {
        String input = "04 Q | BadData";
        FastIcueFrameInputStream in = new FastIcueFrameInputStream(
            new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        
        MalformedFrameException ex = assertThrows(
            MalformedFrameException.class, in::readFrame);
        assertTrue(ex.getMessage().contains("BadData"));
    }

     @Test
    public void testWriteSimpleFrame() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FastIcueFrameOutputStream out = new FastIcueFrameOutputStream(baos);
        FastIcueFrame frame = new FastIcueFrame(1, 'Q', "PING FastICUE/1.0");
        
        out.writeFrame(frame);
        String result = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        
        assertEquals("1 Q | PING FastICUE/1.0\r\n", result);
    }

    @Test
    public void testConstructMultilineFrame() throws Exception {
        assertThrows(AssertionError.class, () -> new FastIcueFrame(2, 'D', "Hello\r\nWorld"));
    }

    @Test
    public void testWriteFrameWithoutLengthWhenNoCRLF() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FastIcueFrameOutputStream out = new FastIcueFrameOutputStream(baos);
        FastIcueFrame frame = new FastIcueFrame(3, 'H', "SafeData");
        
        out.writeFrame(frame);
        String result = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        
        assertEquals("3 H | SafeData\r\n", result);
    }

}
