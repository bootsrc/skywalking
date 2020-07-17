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

import brave.Tracing;
import brave.propagation.CurrentTraceContext;
import brave.propagation.ThreadLocalCurrentTraceContext;
import lombok.Getter;
import org.apache.skywalking.apm.agent.core.boot.OverrideImplementor;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.remote.TraceSegmentServiceClient;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;
import zipkin2.reporter.okhttp3.OkHttpSender;

/**
 * Zipkin traces are reported through brave client. This override implementor majorly make sure the original memory
 * queue and grpc client doesn't work, and set up the Zipkin client in the right way.
 */
@OverrideImplementor(TraceSegmentServiceClient.class)
public class ZipkinTraceReporter extends TraceSegmentServiceClient {
    @Getter
    private Tracing tracing;
    @Getter
    private ThreadLocalCurrentTraceContext currentTraceContext;
    private OkHttpSender sender;
    private AsyncZipkinSpanHandler zipkinSpanHandler;

    @Override
    public void prepare() {
    }

    /**
     * Set up the Zipkin reporter, use {@link Config.Collector#BACKEND_SERVICE} as the report URL. Typically, the path
     * should be http://ip:port/api/v2/spans
     */
    @Override
    public void boot() {
        sender = OkHttpSender.create(Config.Collector.BACKEND_SERVICE);
        zipkinSpanHandler = AsyncZipkinSpanHandler.create(sender);
        currentTraceContext = (ThreadLocalCurrentTraceContext) CurrentTraceContext.Default.create();

        // Create a tracing component with the service name you want to see in Zipkin.
        tracing = Tracing.newBuilder()
                         .currentTraceContext(currentTraceContext)
                         .localServiceName(Config.Agent.SERVICE_NAME)
                         .addSpanHandler(zipkinSpanHandler)
                         .build();
    }

    @Override
    public void shutdown() {
        tracing.close();
        zipkinSpanHandler.close();
        sender.close();

        // This is just cleaning the current thread context, mostly for tests.
        currentTraceContext.clear();
    }
}
