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
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.context.AbstractTracerContext;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.powermock.reflect.Whitebox;
import zipkin2.Span;
import zipkin2.junit.ZipkinRule;

public class ZipkinTracingTest {
    @Rule
    public ZipkinRule zipkin = new ZipkinRule();

    @Before
    public void setup() {
        Config.Collector.BACKEND_SERVICE = zipkin.httpUrl() + "/api/v2/spans";
        Config.Agent.SERVICE_NAME = "Zipkin-Core";
    }

    @After
    public void clean() {
        Config.Collector.BACKEND_SERVICE = null;
        Config.Agent.SERVICE_NAME = null;
    }

    @Test
    public void zipkinTracerRunning() throws Exception {
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

        List<List<Span>> traces = null;
        boolean received = false;
        for (int i = 10; i >= 0; i--) {
            Thread.sleep(2000L);
            traces = zipkin.getTraces();
            if (traces.size() > 0) {
                received = true;
                break;
            }
        }

        Assert.assertTrue(received);
        Assert.assertEquals(1, traces.size());
    }
}
