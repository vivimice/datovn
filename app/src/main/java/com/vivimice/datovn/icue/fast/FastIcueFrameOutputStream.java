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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class FastIcueFrameOutputStream extends FilterOutputStream {

    public FastIcueFrameOutputStream(OutputStream out) {
        super(out);
    }

    public void writeFrame(FastIcueFrame frame) throws IOException {
        // write invocation id
        write(Integer.toHexString(frame.invocationId()).getBytes(StandardCharsets.UTF_8));
        // write seperator 1
        write(0x20);
        // write type
        write((byte) frame.type());
        // write seperator 2
        write(new byte[] {0x20, 0x7c, 0x20});
        // write data
        write(frame.data().getBytes(StandardCharsets.UTF_8));
        // write terminator
        write(new byte[] {0x0d, 0x0a});
    }

}
