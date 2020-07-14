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

import java.util.List;
import org.apache.skywalking.apm.agent.core.context.AbstractTracerContext;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.junit.Assert;
import org.junit.Test;
import org.powermock.reflect.Whitebox;
import zipkin2.Span;

public class ZipkinTracerContextTest extends ZipkinTest {
    @Test
    public void testContextIsolationInThread() {
        final ZipkinTraceReporter zipkinTraceReporter = new ZipkinTraceReporter();
        zipkinTraceReporter.boot();

        ZipkinContextManager manager = new ZipkinContextManager();
        Whitebox.setInternalState(manager, "zipkinTraceReporter", zipkinTraceReporter);
        final AbstractTracerContext traceContext = manager.createTraceContext("/span1", true);
        Assert.assertTrue(traceContext instanceof ZipkinTracerContext);

        final AbstractSpan span1 = traceContext.createEntrySpan("span1");
        final AbstractSpan span2 = traceContext.createLocalSpan("span2");
        final AbstractSpan span3 = traceContext.createExitSpan("span3", "127.0.0.1:8080");
        traceContext.stopSpan(span3);
        traceContext.stopSpan(span2);
        traceContext.stopSpan(span1);

        List<List<Span>> traces = readTracesUntilTimeout(10);
        Assert.assertEquals(1, traces.size());
        List<Span> spans = traces.get(0);
        Assert.assertEquals(3, spans.size());
        Assert.assertEquals("span3", spans.get(0).name());

        // Scope should be clear automatically in traceContext#stopSpan.
        final AbstractSpan span4 = traceContext.createEntrySpan("span4");
        traceContext.stopSpan(span4);

        traces = readTracesUntilTimeout(10);
        Assert.assertEquals(2, traces.size());
        spans = traces.get(1);
        Assert.assertEquals(1, spans.size());
        Assert.assertEquals("span4", spans.get(0).name());
    }
}
