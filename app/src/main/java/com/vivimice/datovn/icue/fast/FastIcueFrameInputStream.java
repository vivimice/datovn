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

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

public class FastIcueFrameInputStream extends FilterInputStream {

    private static final int CONTEXT_BUFFER_SIZE = 16; // size of the context buffer used for debugging purposes.
    private static final String[] ESCAPE_MAP = new String[] {
        "\\0", "\\x01", "\\x02", "\\x03", "\\x04", "\\x05", "\\x06", "\\x07", 
        "\\x08", "\\t", "\\n", "\\x0b", "\\x0c", "\\r", "\\x0e", "\\x0f", 
        "\\x10", "\\x11", "\\x12", "\\x13", "\\x14", "\\x15", "\\x16", "\\x17", 
        "\\x18", "\\x19", "\\x1a", "\\x1b", "\\x1c", "\\x1d", "\\x1e", "\\x1f"
    };

    // contextual ring buffer, used to store the last 16 bytes read from the stream.
    // useful for debugging.
    private byte[] contextRingBuffer = new byte[CONTEXT_BUFFER_SIZE];
    private int ringCursor = 0; // points to the next empty slot of context ring buffer
    private boolean ringClamped = false;

    public FastIcueFrameInputStream(InputStream in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        // update contextual buffer
        if (b >= 0) {
            fillContextBuffer((byte) b);
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int bytesRead = super.read(b, off, len);

        // update contextual buffer
        int n = (bytesRead < CONTEXT_BUFFER_SIZE) ? bytesRead : CONTEXT_BUFFER_SIZE;
        int base = (off + bytesRead - n);
        for (int i = 0; i < n; i++) {
            fillContextBuffer(b[base + i]);
        }

        return bytesRead;
    }

    private void fillContextBuffer(byte b) {
        contextRingBuffer[ringCursor] = b;
        ringCursor++;
        if (ringCursor >= CONTEXT_BUFFER_SIZE) {
            ringClamped = true;
            ringCursor = 0;
        }
    }

    public FastIcueFrame readFrame() throws IOException, MalformedFrameException {
        byte[] b;
        String s;

        int invocationId;
        char type;
        String data;

        // read invocation id
        b = readUntil(new byte[]{(byte) 0x20}); // read until space
        if (b == null) {
            throw new MalformedFrameException(constructErrorMessage("incomplete frame: invocation id missing"));
        }
        s = new String(b, StandardCharsets.UTF_8); // convert to string
        try {
            invocationId = HexFormat.fromHexDigits(s);
        } catch (IllegalArgumentException ex) {
            throw new MalformedFrameException(constructErrorMessage("invalid invocation id: " + s));
        }
        if (invocationId > 0x7FFFFFFF || invocationId < 0) {
            throw new MalformedFrameException(constructErrorMessage("invalid invocation id: " + s));
        }

        // read type
        b = readUntil(new byte[]{(byte) 0x20, (byte) 0x7c, (byte) 0x20}); // read until " | "
        if (b == null) {
            throw new MalformedFrameException(invocationId, constructErrorMessage("incomplete frame: type missing"));
        }
        if (b.length == 0) {
            throw new MalformedFrameException(invocationId, constructErrorMessage("type missing"));
        } else if (b.length > 1) {
            throw new MalformedFrameException(invocationId, constructErrorMessage("oversized type: len=" + b.length));
        } else {
            type = (char) b[0];
        }

        // read frame data until CR-LF
        b = readUntil(new byte[]{(byte) 0x0d, (byte) 0x0a});
        if (b == null) {
            throw new MalformedFrameException(invocationId, constructErrorMessage("incomplete frame data. Frame terminator missing"));
        }
        data = new String(b, StandardCharsets.UTF_8);

        return new FastIcueFrame(invocationId, type, data);
    }

    private String constructErrorMessage(String message) {
        byte[] context = new byte[CONTEXT_BUFFER_SIZE];
        if (ringClamped) {
            System.arraycopy(contextRingBuffer, 0, context, CONTEXT_BUFFER_SIZE - ringCursor, ringCursor);
            System.arraycopy(contextRingBuffer, ringCursor, context, 0, CONTEXT_BUFFER_SIZE - ringCursor);
        } else {
            System.arraycopy(contextRingBuffer, 0, context, 0, ringCursor);
        }

        int indent = 4;
        StringBuilder sb = new StringBuilder(message)
            .append(System.lineSeparator());

        // append escaped message
        String contextStr = new String(context, 0, ringClamped ? CONTEXT_BUFFER_SIZE : ringCursor, StandardCharsets.UTF_8);
        int offset = indent;
        sb.repeat(" ", indent);
        if (ringClamped) {
            sb.append("...");
            offset += 3;
        }
        for (int i = 0; i < contextStr.length(); i++) {
            char ch = contextStr.charAt(i);
            if (ch < ESCAPE_MAP.length) {
                String escape = ESCAPE_MAP[ch];
                sb.append(escape);
                offset += escape.length();
            } else {
                sb.append(ch);
                offset++;
            }
        }
        sb.append(System.lineSeparator());

        sb.repeat(" ", offset).append("^");

        return sb.toString();
    }

    /**
     * Reads until a delimiter is encountered. The delimiter is not included in the returned byte array.
     */
    protected byte[] readUntil(byte[] delimiter) throws IOException {
        assert delimiter != null && delimiter.length > 0;
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        int p = 0;
        while (true) {
            int b = read();
            if (b == -1) {
                return null;
            } else if (b == delimiter[p]) {
                p++;
                if (p == delimiter.length) { // found the delimiter
                    return bo.toByteArray();
                }
            } else {
                if (p > 0) {
                    // write back the bytes that were matches as part of the delimiter sequence
                    bo.write(delimiter, 0, p);
                    // reset
                    p = 0;
                }
                bo.write(b);
            }
        }
    }

}
