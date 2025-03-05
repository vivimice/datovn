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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vivimice.datovn.stage.StageContext;
import com.vivimice.datovn.stage.StageScopeService;

public class FastIcueClientService implements StageScopeService {

    private static final Logger logger = LoggerFactory.getLogger(FastIcueClientService.class);

    private final Map<List<String>, AtomicReference<FastIcueConnection>> connections = new ConcurrentHashMap<>();

    private StageContext context;
    private volatile boolean shutdown = false;

    @Override
    public void onInit(StageContext context) {
        assert context != null;
        this.context = context;
    }

    @Override
    public void onDestroy() {
        shutdown = true;
        logger.info("Closing FastICUE connections ...");
        connections.forEach((name, ref) -> {
            FastIcueConnection connection = ref.get();
            synchronized (ref) {
                if (connection != null) {
                    logger.debug("Closing FastICUE connection: {}", name);
                    connection.close();
                    ref.set(null);
                }
            }
        });
    }

    public FastIcueClient get(List<String> command) throws FastIcueClientException {
        if (shutdown) {
            logger.warn("Cannot get FastICUE client as service is shutting down.");
            return null;
        }

        AtomicReference<FastIcueConnection> ref = connections.computeIfAbsent(command, cmd -> new AtomicReference<>());
        synchronized (ref) {
            FastIcueConnection connection = ref.get();
            if (connection != null) {
                logger.info("Reusing existing FastICUE connection");
            } else {
                logger.info("Creating new FastICUE connection with command: {}", command);
                connection = new FastIcueConnection(context, command);
                connection.start();
                ref.set(connection);
            }

            try {
                return connection.getClient();
            } catch (FastIcueClientException ex) {
                logger.info("Shutting down failed FastICUE connection.");
                connection.close();
                ref.set(null);
                throw ex;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new FastIcueClientException("Interrupted while waiting for FastICUE client.");
            }
        }
    }

}
