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
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import zipkin2.Span;

import static org.apache.skywalking.apm.plugin.reporter.zipkin.ZipkinTracerContext.B3_NAME_SAMPLED;
import static org.apache.skywalking.apm.plugin.reporter.zipkin.ZipkinTracerContext.B3_NAME_SPAN_ID;
import static org.apache.skywalking.apm.plugin.reporter.zipkin.ZipkinTracerContext.B3_NAME_TRACE_ID;

public class ContextManageTest extends ZipkinTest {
    @AfterClass
    public static void shutdown() {
        ServiceManager.INSTANCE.shutdown();
    }

    @Test
    public void testInThread() {
        final AbstractSpan entrySpan = ContextManager.createEntrySpan("/span", new ContextCarrier());
        Assert.assertTrue(entrySpan instanceof ZipkinSpan);
        ContextManager.stopSpan();

        final List<List<Span>> traces = readTracesUntilTimeout(10, 1, 1);
        Assert.assertEquals(1, traces.size());
        final List<Span> spans = traces.get(0);
        Assert.assertEquals(1, spans.size());
        Assert.assertEquals("/span", spans.get(0).name());
    }

    @Test
    public void testAcrossProcess() {
        final ContextCarrier header = new ContextCarrier();
        ContextManager.createLocalSpan("local method");
        ContextManager.createExitSpan("/rpc-client", header, "127.0.0.1");
        ContextManager.stopSpan();
        ContextManager.stopSpan();

        List<List<Span>> traces = readTracesUntilTimeout(10, 1, 2);
        Assert.assertEquals(1, traces.size());

        Assert.assertNotNull(header.readCustomContext(B3_NAME_SPAN_ID));
        Assert.assertNotNull(header.readCustomContext(B3_NAME_SAMPLED));
        Assert.assertNotNull(header.readCustomContext(B3_NAME_TRACE_ID));

        ContextManager.createEntrySpan("/rpc-server", header);
        ContextManager.stopSpan();

        traces = readTracesUntilTimeout(10, 1, 3);
        Assert.assertEquals(1, traces.size());
        final List<Span> spans = traces.get(0);
        Assert.assertEquals(zipkin2.Span.Kind.CLIENT, spans.get(0).kind());
        Assert.assertEquals("/rpc-client", spans.get(0).name());
        Assert.assertEquals(zipkin2.Span.Kind.SERVER, spans.get(2).kind());
        Assert.assertEquals("/rpc-server", spans.get(2).name());
    }
}
