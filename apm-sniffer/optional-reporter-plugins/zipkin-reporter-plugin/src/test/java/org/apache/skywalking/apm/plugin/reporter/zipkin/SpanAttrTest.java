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

import java.util.HashMap;
import java.util.List;
import org.apache.skywalking.apm.agent.core.context.AbstractTracerContext;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.junit.Assert;
import org.junit.Test;
import org.powermock.reflect.Whitebox;
import zipkin2.Span;

public class SpanAttrTest extends ZipkinTest {
    @Test
    public void testSpan() {
        final AbstractTracerContext traceContext = newTracerContext("/span1");
        Assert.assertTrue(traceContext instanceof ZipkinTracerContext);

        final AbstractSpan span1 = traceContext.createEntrySpan("span1");
        span1.tag(Tags.URL, "http://127.0.0.1:8080/query");
        span1.tag(Tags.STATUS_CODE, "200");
        span1.log(new RuntimeException());
        span1.setComponent(ComponentsDefine.TOMCAT);
        span1.setLayer(SpanLayer.HTTP);
        // All these methods for now will be ignored, because these are not suitable or recommended in the Zipkin models.
        span1.errorOccurred();
        span1.log(System.currentTimeMillis(), new HashMap<>());
        //
        traceContext.stopSpan(span1);

        List<List<Span>> traces = readTracesUntilTimeout(10, 1, 1);
        Assert.assertEquals(1, traces.size());
        final List<Span> spans = traces.get(0);
        Assert.assertEquals(1, spans.size());
        final Span span = spans.get(0);
        Assert.assertNotNull(span.traceId());
        Assert.assertEquals(Span.Kind.SERVER, span.kind());
        Assert.assertEquals(5, span.tags().size());
        Assert.assertEquals("RuntimeException", span.tags().get("error"));
        Assert.assertEquals("200", span.tags().get("status_code"));
        Assert.assertEquals("http://127.0.0.1:8080/query", span.tags().get("url"));
        Assert.assertEquals("Tomcat", span.tags().get("component"));
        Assert.assertEquals("HTTP", span.tags().get("layer"));
    }

    private AbstractTracerContext newTracerContext(String entranceEndpoint) {
        final ZipkinTraceReporter zipkinTraceReporter = new ZipkinTraceReporter();
        zipkinTraceReporter.boot();

        ZipkinContextManager manager = new ZipkinContextManager();
        Whitebox.setInternalState(manager, "zipkinTraceReporter", zipkinTraceReporter);
        return manager.createTraceContext(entranceEndpoint, true);
    }
}
