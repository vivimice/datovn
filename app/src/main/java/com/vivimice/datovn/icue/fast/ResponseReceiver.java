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

import static com.vivimice.datovn.icue.fast.FastIcueConstants.FRAME_TYPE_BASE64_DATA;
import static com.vivimice.datovn.icue.fast.FastIcueConstants.FRAME_TYPE_LINE_DATA;
import static com.vivimice.datovn.icue.fast.FastIcueConstants.FRAME_TYPE_RESPONSE_STATUS;
import static com.vivimice.datovn.icue.fast.FastIcueConstants.FRAME_TYPE_TERMINATION;
import static com.vivimice.datovn.icue.fast.FastIcueConstants.PROTOCOL;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ResponseReceiver {

    private static final Logger logger = LoggerFactory.getLogger(ResponseReceiver.class);

    private static enum State {
        READ_STATUS_LINE, READ_BODY, ERROR, OK;
    }

    private final StringBuilder dataBuilder = new StringBuilder();
    private State state;
    private int statusCode;
    private String statusMessage;
    private FastIcueInvocationException error;

    private Object responseSynchronizer = new Object(); // Used to synchronize access to the response object
    private FastIcueResponse response = null;

    public void addFrame(FastIcueFrame frame) {
        if (state == State.ERROR) {
            // Ignore any frames after an error has been detected
            return;
        }

        switch (frame.type()) {
            case FRAME_TYPE_RESPONSE_STATUS:
                addResponseStatusFrame(frame);
                break;
            case FRAME_TYPE_LINE_DATA:
            case FRAME_TYPE_BASE64_DATA:
                addDataFrame(frame);
                break;
            case FRAME_TYPE_TERMINATION:
                addTerminationFrame();
                break;
            default:
                setError("Unexpected response frame: " + frame.type());
        }
    }

    private void addResponseStatusFrame(FastIcueFrame frame) {
        if (state != State.READ_STATUS_LINE) {
            setError("Unexpected response frame: " + frame.data());
            return;
        }

        String data = frame.data();
        String[] parts = data.split("\\s+", 3);
        if (parts.length != 3) {
            setError("Invalid status frame: " + data);
            return;
        }

        String protocol = parts[0];
        if (!Objects.equals(protocol, PROTOCOL)) {
            setError("Unsupported protocol: " + protocol);
            return;
        }

        String statusCodeStr = parts[1];
        if (!statusCodeStr.matches("\\d{3}")) {
            setError("Invalid status code: " + statusCodeStr);
            return;
        }

        int statusCode;
        try {
            statusCode = Integer.parseInt(statusCodeStr);
        } catch (NumberFormatException ex) {
            // should never happen
            setError("Invalid status code: " + statusCodeStr);
            return;
        }

        String statusMessage = parts[2];

        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.state = State.READ_BODY;
    }

    private void addDataFrame(FastIcueFrame frame) {
        if (this.state != State.READ_BODY) {
            setError("Unexpected data frame: " + frame.data());
            return;
        }

        String data;
        if (frame.type() == FRAME_TYPE_LINE_DATA) {
            data = frame.data() + System.lineSeparator();
        } else if (frame.type() == FRAME_TYPE_BASE64_DATA) {
            data = new String(Base64.getDecoder().decode(frame.data()), StandardCharsets.UTF_8);
        } else {
            // should not happen
            setError("Unexpected frame type: " + frame.type());
            return;
        }

        this.dataBuilder.append(data);
    }

    private void addTerminationFrame() {
        switch (state) {
            case READ_BODY:
            case READ_STATUS_LINE:
                // Normal termination, we have the full response
                state = State.OK;
                if (response == null) {
                    synchronized (responseSynchronizer) {
                        response = new FastIcueResponse(statusCode, statusMessage, dataBuilder.toString());
                        responseSynchronizer.notifyAll();
                    }
                }
                break;
            case OK:
                // Duplicate termination frame, log error
                setError("Duplicate termination frame");
                break;
            default:
                // should never happen
                throw new AssertionError();
        }
    }

    public FastIcueResponse get() throws FastIcueInvocationException, InterruptedException {
        synchronized (responseSynchronizer) {
            while (true) {
                if (error != null) {
                    throw error;
                } else if (response != null) {
                    return response;
                } else {
                    responseSynchronizer.wait();
                }
            }
        }
    }

    public void setError(String message) {
        logger.warn("invocation error: {}", message);
        state = State.ERROR;
        synchronized (responseSynchronizer) {
            error = new FastIcueInvocationException(message);
            responseSynchronizer.notifyAll();
        }
    }

}
