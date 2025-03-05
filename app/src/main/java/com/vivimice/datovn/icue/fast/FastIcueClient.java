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

import static com.vivimice.datovn.icue.fast.FastIcueConstants.FRAME_TYPE_HEADER;
import static com.vivimice.datovn.icue.fast.FastIcueConstants.FRAME_TYPE_REQUEST;
import static com.vivimice.datovn.icue.fast.FastIcueConstants.FRAME_TYPE_TERMINATION;
import static com.vivimice.datovn.icue.fast.FastIcueConstants.PROTOCOL;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is thread-safe.
 */
public class FastIcueClient implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(FastIcueClient.class);
    private static final AtomicInteger INVOCATION_ID_GENERATOR = new AtomicInteger(0);

    private final FastIcueFrameInputStream input;
    private final FastIcueFrameOutputStream output;
    private final Map<Integer, ResponseReceiver> receivers = new ConcurrentHashMap<>();

    private volatile boolean closed;

    public FastIcueClient(InputStream daemonStdout, OutputStream daemonStdin) {
        this.input = new FastIcueFrameInputStream(daemonStdout);
        this.output = new FastIcueFrameOutputStream(daemonStdin);
        this.closed = false;
    }

    public boolean isClosed() {
        return closed;
    }

    public void dispatchFrames() throws IOException {
        logger.info("Starting to dispatch frames ...");
        try {
            while (!closed) {
                try {
                    FastIcueFrame frame = input.readFrame();
                    logger.trace("Received frame: {}", frame);

                    // route the frame to the appropriate receiver
                    ResponseReceiver receiver = receivers.get(frame.invocationId());
                    if (receiver != null) {
                        receiver.addFrame(frame);
                    } else {
                        logger.warn("Discarded dangled frame with invocation id: {}", frame.invocationId());
                    }
                } catch (MalformedFrameException ex) {
                    ex.getInvocationId().ifPresentOrElse(
                        invocationId -> logger.warn("Discarded malformed frame with invocation id: {}", invocationId, ex),
                        () -> logger.warn("Discarded malformed frame", ex)
                    );
                }
            }
        } catch (IOException ex) {
            close();
            // notify all responses
            for (ResponseReceiver receiver : receivers.values()) {
                receiver.setError("connection i/o error");
            }
            throw ex;
        }
    }

    private void sendFrame(FastIcueFrame frame) throws IOException {
        try {
            synchronized (output) {
                logger.trace("Sending frame: {}", frame);
                output.writeFrame(frame);
            }
        } catch (IOException ex) {
            throw new IOException("i/o error while write frame", ex);
        }
    }

    public FastIcueResponse invoke(FastIcueRequest request) throws FastIcueInvocationException {
        logger.debug("Invoking FastIcue with request: {}", request);
        if (closed) {
            throw new FastIcueInvocationException("connection closed");
        }

        return doInvoke(request);
    }

    private FastIcueResponse doInvoke(FastIcueRequest request) throws FastIcueInvocationException {
        int invocationId = INVOCATION_ID_GENERATOR.incrementAndGet();
        ResponseReceiver receiver = receivers.computeIfAbsent(invocationId, id -> new ResponseReceiver());

        // Send the request frames
        try {
            // Request frame
            logger.trace("Sending request frame");
            sendFrame(new FastIcueFrame(invocationId, FRAME_TYPE_REQUEST, request.getMethod() + " " + PROTOCOL));
            // Header frames
            logger.trace("Sending header frames");
            for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
                String headerName = entry.getKey();
                String headerValue = entry.getValue();
                sendFrame(new FastIcueFrame(invocationId, FRAME_TYPE_HEADER, headerName + ": " + headerValue));
            }
            // Terminator frame
            logger.trace("Sending terminator frame");
            sendFrame(new FastIcueFrame(invocationId, FRAME_TYPE_TERMINATION, ""));
        } catch (IOException ex) {
            throw new FastIcueInvocationException("i/o error while sending request", ex);
        }

        // Wait for response
        try {
            logger.debug("Waiting for response");
            return receiver.get();
        } catch (FastIcueInvocationException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            throw new FastIcueInvocationException("interrupted while waiting for response.", ex);
        }
    }

    @Override
    public void close() {
        closed = true;

        // Send TERM request to ask the daemon to terminate gracefully
        // NOTE: Will wait until all pending requests are processed.
        logger.info("Sending TERM request ...");
        try {
            invoke(new FastIcueRequest("TERM"));
        } catch (FastIcueInvocationException ex) {
            logger.error("Error sending TERM request", ex);
        }

        try {
            input.close();
        } catch (IOException ex) {
            // ignored
        }

        try {
            output.close();
        } catch (IOException ex) {
            // ignored
        }
    }

}
