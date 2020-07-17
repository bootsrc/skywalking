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

import org.apache.skywalking.apm.agent.core.boot.OverrideImplementor;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.context.AbstractTracerContext;
import org.apache.skywalking.apm.agent.core.context.ContextManagerExtendService;
import org.apache.skywalking.apm.agent.core.remote.TraceSegmentServiceClient;

/**
 * ZipkinContextManager used Brave APIs to manage the Zipkin tracing context, including span start/stop/tag/log,
 * inject/extract in across process, and capture/continue in across thread.
 */
@OverrideImplementor(ContextManagerExtendService.class)
public class ZipkinContextManager extends ContextManagerExtendService {
    private ZipkinTraceReporter zipkinTraceReporter;

    @Override
    public void prepare() {
        zipkinTraceReporter = (ZipkinTraceReporter) ServiceManager.INSTANCE.findService(
            TraceSegmentServiceClient.class);
    }

    @Override
    public void shutdown() {
        zipkinTraceReporter = null;
    }

    /**
     * Create AbstractTracerContext with as all new Zipkin tracer.
     */
    @Override
    public AbstractTracerContext createTraceContext(final String operationName, final boolean forceSampling) {
        return new ZipkinTracerContext(zipkinTraceReporter.getTracing(), zipkinTraceReporter.getCurrentTraceContext());
    }
}
