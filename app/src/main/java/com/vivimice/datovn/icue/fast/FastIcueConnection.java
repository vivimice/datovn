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

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC.MDCCloseable;

import com.vivimice.datovn.stage.StageContext;

public class FastIcueConnection extends Thread {

    private static enum State {
        NEW, CONNECTING, CONNECTED, PROCESS_FAILURE, STOPPING, STOPPED;
    }

    private static final Logger logger = LoggerFactory.getLogger(FastIcueConnection.class);

    private final StageContext context;
    private final List<String> command;

    private Process process;
    private FastIcueClient client;
    private volatile State state;

    public FastIcueConnection(StageContext context, List<String> command) {
        assert context != null;
        assert command != null;
        this.context = context;
        this.command = command;
        this.state = State.NEW;

        setName("[" + context.getStageName() + "] FastICUE Daemon: " + String.join(" ", command));
    }

    public long getPid() {
        if (process != null) {
            return process.pid();
        } else {
            return -1;
        }
    }

    @Override
    public void run() {
        try (MDCCloseable mdc = context.putMdcClosable()) {
            try {
                process = new ProcessBuilder()
                        .command(command)
                        .directory(context.getStageWorkingDir().toFile())
                        .redirectError(Redirect.DISCARD)
                        .start();
            } catch (IOException ex) {
                logger.error("Failed to start FastICUE daemon", ex);
                setState(State.PROCESS_FAILURE);
                return;
            }
                    
            client = new FastIcueClient(process.getInputStream(), process.getOutputStream());
            setState(State.CONNECTED);  // Once state is CONNECTED, client is available
            synchronized (this) {
                notifyAll();
            }

            try {
                client.dispatchFrames();
            } catch (IOException ex) {
                logger.error("i/o error communicating with FastICUE daemon");
            }
        } finally {
            setState(State.STOPPING);
            if (process != null) {
                logger.info("Stopping FastICUE daemon");
                process.destroy();
                try {
                    process.waitFor();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    logger.error("Interrupted while waiting for FastICUE daemon to finish");
                }
                process = null;
            }

            client = null;
            setState(State.STOPPED);
            logger.info("FastICUE daemon stopped");
        }
    }

    private void setState(State state) {
        this.state = state;
        synchronized (this) {
            notifyAll();
        }
    }

    /**
     * Returns the client for communicating with the FastICUE daemon. Waits if current
     * state is CONNECTING. If such client cannot be obtained, throws an exception.
     * 
     * @return the client for communicating with the FastICUE daemon. Won't be null.
     * @throws FastIcueClientException
     * @throws InterruptedException
     */
    public FastIcueClient getClient() throws FastIcueClientException, InterruptedException {
        // Wait until the state is no longer CONNECTING
        State currentState;
        synchronized (this) {
            while (true) {
                currentState = this.state;
                if (currentState == State.CONNECTING) {
                    wait();
                } else {
                    break;
                }
            }
        }

        switch (currentState) {
            case NEW:
                throw new FastIcueClientException("FastICUE daemon is not started yet");
            case CONNECTED:
                break;
            case STOPPING:
                throw new FastIcueClientException("FastICUE daemon is stopping");
            case STOPPED:
                throw new FastIcueClientException("FastICUE daemon is stopped");
            default:
                // should never happen
                throw new AssertionError();
        }

        FastIcueResponse response;
        try {
            logger.debug("Sending ping request to FastICUE daemon ...");
            response = client.invoke(new FastIcueRequest("PING"));
        } catch (FastIcueInvocationException ex) {
            throw new FastIcueClientException("FastICUE daemon not responding", ex);
        }

        if (response.statusCode() != FastIcueConstants.STATUS_OK) {
            throw new FastIcueClientException("FastICUE daemon not responding with a valid status code: " + response.statusCode());
        }

        return client;
    }

    public void close() {
        if (state != State.STOPPED && state != State.STOPPING) {
            return;
        }

        setState(State.STOPPING);

        FastIcueClient client = this.client;
        if (client != null) {
            logger.debug("Closing connection to FastICUE daemon");
            client.close();
        }

        Process process = this.process;
        if (process != null) {
            logger.debug("Destroying FastICUE daemon process");
            process.destroy();
        }
    }

}
