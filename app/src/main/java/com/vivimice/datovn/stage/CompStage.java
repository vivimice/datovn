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
package com.vivimice.datovn.stage;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.MDC.MDCCloseable;

import com.vivimice.datovn.DatovnRuntimeException;
import com.vivimice.datovn.action.ActionsStore;
import com.vivimice.datovn.action.LoadedSketches;
import com.vivimice.datovn.action.MessageLevel;
import com.vivimice.datovn.profiler.ProfilerCloseable;
import com.vivimice.datovn.profiler.UnitProfiler;
import com.vivimice.datovn.spec.CompExecSpec;
import com.vivimice.datovn.unit.CompActionRecorder;
import com.vivimice.datovn.unit.CompUnit;
import com.vivimice.datovn.unit.CompUnits;
import com.vivimice.datovn.unit.UnitContext;
import com.vivimice.datovn.util.OffendingPathAccessChecker;

public class CompStage {

    private final static Logger logger = LoggerFactory.getLogger(CompStage.class);

    private final StageContext context;
    private final Set<String> scheduledSpecs = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<String, StageScopeService> services = new ConcurrentHashMap<>();

    // Note: Path access checker is not thread-safe.
    private final OffendingPathAccessChecker<String> pathAccessChecker = new OffendingPathAccessChecker<>();

    // Variables that must be protected by <code>synchronized (this) {}</code> block
    // Number of remaining unfinished computations
    private int remainExecutions = 0;
    private int totalExecutions = 0;
    private boolean started = false;

    public CompStage(StageContext context) {
        assert context != null;
        this.context = context;
    }

    /**
     * Start the computation with the initial specification. The specification might
     * execute subsequent specification(s) during the computation. This method will
     * wait for all subsequent specifications to finish.
     * 
     * @param initialSpec the initial specification within this stage
     * @throws IllegalStateException if already started
     * @throws NullPointerException if initialSpec is <code>null</code>
     */
    public void start(CompExecSpec initialSpec) {
        try (
            MDCCloseable stageMdcc = context.putMdcClosable();
            ProfilerCloseable pc = context.getProfiler().wrapStageRun(context.getStageName());
        ) {
            assert initialSpec != null;

            logger.info("Stage started");
            
            synchronized (this) {
                // Check if already started
                assert !started : "Stage already started";
                started = true;
                totalExecutions = 0;

                // Schedule the initial computation
                schedule(initialSpec);

                // Wait for all unfinished computations to finish
                while (remainExecutions > 0) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            // Destroy all services after finishing the stage
            services.forEach((name, service) -> {
                logger.debug("Destroying service: {}", name);
                service.onDestroy();
            });

            logger.info("Stage finished");
        }
    }

    private void schedule(CompExecSpec spec) {
        boolean unscheduled = scheduledSpecs.add(spec.getName()); // implict null-check
        if (!unscheduled) {
            // If the spec is already scheduled, raise an error
            throw new DatovnRuntimeException("Spec '" + spec.getName() + "' already scheduled in the same stage. Duplicate specs with same names in the same stage are not allowed.");
        }

        logger.debug("Scheduling new exec spec: {}", spec);

        UnitProfiler profiler = context.getProfiler().createUnitProfiler();
        profiler.onSchedule(spec);
        
        Executor threadPool = context.getCompUnitThreadPool();
        synchronized (this) {
            remainExecutions++;
            totalExecutions++;
            threadPool.execute(() -> {
                try (MDCCloseable stageMdcc = MDC.putCloseable("stage", context.getStageName())) {
                    logger.debug("Starting computation ...");
                    
                    // report progress
                    double progress = 1d * (totalExecutions - remainExecutions) / totalExecutions;
                    context.logProgress(progress, "Building unit: " + spec.getName());
                    
                    UnitContext unitContext = new UnitContextImpl(context, profiler, spec, services);
                    try (
                        MDCCloseable unitMdcc = MDC.putCloseable("unit", spec.getName());
                        ProfilerCloseable pc = profiler.wrapExecution();
                    ) {
                        // execute computation
                        execute(spec, unitContext);
                    } catch (DatovnRuntimeException ex) {
                        // log and handle expected runtime exceptions
                        unitContext.logMessage(MessageLevel.FATAL, ex.getMessage(), null);
                        if (ex.getCause() != null) {
                            logger.warn("Datovn runtime exception", ex);
                        }
                    } catch (RuntimeException ex) {
                        // log and handle unhandled runtime exception, as last resort
                        unitContext.logMessage(MessageLevel.FATAL, "internal error: " + ex.getMessage(), null);
                        logger.warn("Unhandled runtime exception during execution", ex);
                    } finally {
                        logger.debug("Computation finished");
                        synchronized (this) {
                            remainExecutions--;
                            notifyAll();
                        }
                    }
                }
            });
        }
    }

    /**
     * Execute the computation.
     */
    private void execute(CompExecSpec spec, UnitContext execContext) {
        // Create computation unit from specification
        CompUnit unit = CompUnits.create(spec);
        ActionsStore actionsStore = context.getActionsStore();
        CompActionProcessor actionProcessor = new CompActionProcessor(context, execContext, pathAccessChecker, spec);
        UnitProfiler profiler = execContext.getProfiler();

        logger.debug("Reading previous action sketches");
        LoadedSketches prev;
        boolean upToDate;
        try (ProfilerCloseable pc = profiler.wrapLoadSketches()) { 
            prev = actionsStore.loadActionSketches(spec);
            upToDate = (prev != null);
            pc.set("upToDate", upToDate);
            if (prev != null) {
                pc.set("updateTime", prev.updateTime());
            }
        }

        if (upToDate) {
            logger.debug("Computation is up-to-date.");
            prev.sketches().forEach(actionProcessor::accept);
        } else {
            // Execute CompUnit and collect action sketches reported during the computation.
            logger.debug("Computation is out-of-date. Execute computation from scratch.");
            try (ProfilerCloseable pc = profiler.wrapUnitRun()) {
                unit.execute(execContext, new CompActionRecorder(actionProcessor));
            }
        }

        // Report addition errors during action recording
        for (ProcessingError pe : actionProcessor.processingErrors) {
            execContext.logMessage(MessageLevel.ERROR, pe.message(), pe.location());
        }

        // Write sketches to store, if out-of-date.
        if (!upToDate) {
            logger.debug("Writing action sketches to store.");
            try (ProfilerCloseable pc = profiler.wrapWriteSketches()) {
                actionsStore.writeActionSketches(spec, actionProcessor.recordedSketches);
                pc.set("count", actionProcessor.recordedSketches.size());
            }
        }

        // Schedule subsequent computations
        logger.debug("Scheduling subsequent computations.");
        for (CompExecSpec subExecSpec : actionProcessor.getInvocations()) {
            schedule(subExecSpec);
        }
    }
}
