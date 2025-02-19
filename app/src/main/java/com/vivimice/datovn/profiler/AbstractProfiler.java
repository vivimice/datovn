/*
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
package com.vivimice.datovn.profiler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public abstract class AbstractProfiler {

    protected final ProfilerContext context;
    private final Map<String, Object> commonEventData = new LinkedHashMap<>();

    public AbstractProfiler(Consumer<ProfileEvent> eventConsumer) {
        this(new ProfilerContextImpl(eventConsumer));
    }

    protected AbstractProfiler(ProfilerContext context) {
        assert context != null;
        this.context = context;
    }

    protected void setCommonEventData(String key, Object value) {
        commonEventData.put(key, value);
    }

    protected void emitEvent(String name) {
        emitEvent(name, Map.of());
    }

    protected void emitEvent(String name, Map<String, Object> data) {
        Map<String, Object> merged = new LinkedHashMap<>(commonEventData);
        merged.putAll(data);

        ProfileEvent event = new ProfileEvent(context.getClock(), name, merged);
        context.getEventConsumer().accept(event);
    }
    
    protected ProfilerCloseable wrapEvent(String name) {
        return wrapEvent(name, Map.of());
    }

    protected ProfilerCloseable wrapEvent(String name, Map<String, Object> initialData) {
        emitEvent(name + ":start", initialData);
        return ProfilerCloseable.of((data) -> emitEvent(name + ":end", data));
    }

    static class ProfilerContextImpl implements ProfilerContext {

        private final long startNanos = System.nanoTime();
        private final Consumer<ProfileEvent> eventConsumer;
        private final AtomicInteger idCounter = new AtomicInteger();

        ProfilerContextImpl(Consumer<ProfileEvent> eventConsumer) {
            this.eventConsumer = eventConsumer;
        }

        @Override
        public Consumer<ProfileEvent> getEventConsumer() {
            return eventConsumer;
        }

        @Override
        public long getClock() {
            return System.nanoTime() - startNanos;
        }

        @Override
        public int nextId() {
            return idCounter.incrementAndGet();
        }

    }

}
