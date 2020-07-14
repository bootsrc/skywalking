/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.apm.plugin.reporter.zipkin;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.propagation.ThreadLocalCurrentTraceContext;
import brave.propagation.TraceContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.skywalking.apm.agent.core.context.AbstractTracerContext;
import org.apache.skywalking.apm.agent.core.context.AsyncSpan;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.CorrelationContext;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;

/**
 * ZipkinTracerContext is an API wrapper of Zipkin tracer.
 *
 * Same as SkyWalking, Zipkin traceContext is controlled automatically through stack push/pop. Once all created spans
 * finished, the tracer context closed automatically too.
 *
 * Span finished means {@link #stopSpan(AbstractSpan)} called, even in async case, Zipkin span is still alive, and
 * waiting for {@link #asyncStop(AsyncSpan)}.
 */
public class ZipkinTracerContext implements AbstractTracerContext {
    private static String B3_NAME = "b3";

    /**
     * Running span cache of the current Zipkin context. This takes the responsibility of determining when this context
     * should be closed. The key time point is all running context has been closed.
     */
    private Map<Span, ZipkinSpan> runningSpans;
    private final Tracing tracing;
    private final ThreadLocalCurrentTraceContext currentTraceContext;
    private final Tracer tracer;
    private final CorrelationContext correlationContext;

    public ZipkinTracerContext(final Tracing tracing, final ThreadLocalCurrentTraceContext currentTraceContext) {
        this.tracing = tracing;
        this.tracer = tracing.tracer();
        this.currentTraceContext = currentTraceContext;
        runningSpans = new ConcurrentHashMap<>();
        this.correlationContext = new CorrelationContext();
    }

    @Override
    public void inject(final ContextCarrier carrier) {
        tracing.propagation().injector((request, key, value) -> carrier.addCustomContext(key, value))
               .inject(tracing.currentTraceContext().get(), null);

        this.correlationContext.inject(carrier);
    }

    @Override
    public void extract(final ContextCarrier carrier) {
        carrier.setCustomKeys(B3_NAME);
        tracing.propagation().extractor((request, key) -> carrier.readCustomContext(key)).extract(null);
        this.correlationContext.extract(carrier);
    }

    @Override
    public ContextSnapshot capture() {
        final TraceContext traceContext = tracing.currentTraceContext().get();
        ContextSnapshot contextSnapshot = new ContextSnapshot(correlationContext);
        contextSnapshot.addCustomContext(B3_NAME, traceContext);
        return contextSnapshot;
    }

    @Override
    public void continued(final ContextSnapshot snapshot) {
        final TraceContext traceContext = (TraceContext) snapshot.readCustomContext(B3_NAME);
        tracing.currentTraceContext().newScope(traceContext);
    }

    @Override
    public String getReadablePrimaryTraceId() {
        final Span span = tracer.currentSpan();
        return span == null ? "N/A" : span.context().traceIdString();
    }

    @Override
    public AbstractSpan createEntrySpan(final String operationName) {
        final Span span = tracer.nextSpan().name(operationName);
        return createOrGet(span).setEntry(true);
    }

    @Override
    public AbstractSpan createLocalSpan(final String operationName) {
        final Span span = tracer.nextSpan().name(operationName);
        return createOrGet(span);
    }

    @Override
    public AbstractSpan createExitSpan(final String operationName, final String remotePeer) {
        final Span span = tracer.nextSpan().name(operationName);
        span.remoteServiceName(remotePeer);
        return createOrGet(span).setExit(true);
    }

    @Override
    public AbstractSpan activeSpan() {
        final Span span = tracer.currentSpan();
        if (span == null) {
            throw new IllegalStateException("No active span.");
        }
        return createOrGet(span);
    }

    /**
     * @param span to finish
     * @return true once no active span.
     */
    @Override
    public boolean stopSpan(final AbstractSpan span) {
        final ZipkinSpan zipkinSpan = (ZipkinSpan) span;
        if (!zipkinSpan.isAsync()) {
            zipkinSpan.stop();
        }
        runningSpans.remove(zipkinSpan.getSpan());
        boolean isContextFinished = runningSpans.isEmpty();
        if (isContextFinished) {
            currentTraceContext.clear();
        }
        return isContextFinished;
    }

    @Override
    public AbstractTracerContext awaitFinishAsync() {
        return this;
    }

    @Override
    public void asyncStop(final AsyncSpan span) {
        ((ZipkinSpan) span).stop();
    }

    @Override
    public CorrelationContext getCorrelationContext() {
        return null;
    }

    private ZipkinSpan createOrGet(Span span) {
        ZipkinSpan zipkinSpan = runningSpans.get(span);
        if (zipkinSpan == null) {
            zipkinSpan = new ZipkinSpan(span);
            final ZipkinSpan prevValue = runningSpans.putIfAbsent(span, zipkinSpan);
            if (prevValue != null) {
                throw new IllegalStateException("No concurrency access for span creation");
            }
            currentTraceContext.maybeScope(span.context());
        }
        return zipkinSpan;
    }
}
