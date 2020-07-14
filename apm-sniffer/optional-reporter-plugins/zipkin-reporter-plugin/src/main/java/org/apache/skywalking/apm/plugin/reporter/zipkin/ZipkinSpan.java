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
import java.util.Map;
import lombok.Getter;
import org.apache.skywalking.apm.agent.core.context.tag.AbstractTag;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegmentRef;
import org.apache.skywalking.apm.network.trace.component.Component;

/**
 * Zipkin span is the API bridge for Zipkin span running in the SkyWalking shell.
 */
@Getter
public class ZipkinSpan implements AbstractSpan {
    private final Span span;
    private boolean isEntry;
    private boolean isExit;
    @Getter
    private boolean isAsync = false;

    public ZipkinSpan(final Span span) {
        this.span = span;
        this.isEntry = false;
        this.isExit = false;
    }

    public ZipkinSpan setEntry(final boolean entry) {
        isEntry = entry;
        span.kind(Span.Kind.SERVER);
        return this;
    }

    public ZipkinSpan setExit(final boolean exit) {
        isExit = exit;
        span.kind(Span.Kind.CLIENT);
        return this;
    }

    @Override
    public AbstractSpan setComponent(final Component component) {
        span.tag("component", component.getName());
        return this;
    }

    @Override
    public AbstractSpan setLayer(final SpanLayer layer) {
        span.tag("layer", layer.name());
        if (isEntry && layer.equals(SpanLayer.MQ)) {
            span.kind(Span.Kind.CONSUMER);
        } else if (isExit && layer.equals(SpanLayer.MQ)) {
            span.kind(Span.Kind.PRODUCER);
        }
        return this;
    }

    @Override
    public AbstractSpan tag(final String key, final String value) {
        span.tag(key, value);
        return this;
    }

    @Override
    public AbstractSpan tag(final AbstractTag<?> tag, final String value) {
        span.tag(tag.key(), value);
        return this;
    }

    @Override
    public AbstractSpan log(final Throwable t) {
        span.error(t);
        return this;
    }

    @Override
    public AbstractSpan errorOccurred() {
        return this;
    }

    @Override
    public boolean isEntry() {
        return isEntry;
    }

    @Override
    public boolean isExit() {
        return isExit;
    }

    @Override
    public AbstractSpan log(final long timestamp, final Map<String, ?> event) {
        return null;
    }

    @Override
    public AbstractSpan setOperationName(final String operationName) {
        span.name(operationName);
        return this;
    }

    @Override
    public AbstractSpan start() {
        span.start();
        return this;
    }

    /**
     * @return 0 always, as span is not readable before finished.
     */
    @Override
    public int getSpanId() {
        return 0;
    }

    /**
     * @return empty string, as span is not readable before finished.
     */
    @Override
    public String getOperationName() {
        return "";
    }

    @Override
    public void ref(final TraceSegmentRef ref) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AbstractSpan start(final long startTime) {
        span.start(startTime);
        return this;
    }

    @Override
    public AbstractSpan setPeer(final String remotePeer) {
        span.remoteServiceName(remotePeer);
        return this;
    }

    @Override
    public boolean isProfiling() {
        return false;
    }

    @Override
    public void skipAnalysis() {

    }

    @Override
    public AbstractSpan prepareForAsync() {
        isAsync = true;
        return this;
    }

    @Override
    public AbstractSpan asyncFinish() {
        span.finish();
        return this;
    }

    public void stop() {
        span.finish();
    }
}
