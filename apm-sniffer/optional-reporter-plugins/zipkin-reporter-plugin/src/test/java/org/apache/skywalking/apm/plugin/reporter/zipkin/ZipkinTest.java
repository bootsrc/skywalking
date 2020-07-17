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

import java.io.IOException;
import java.util.List;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.mockito.internal.util.reflection.Whitebox;
import zipkin2.Span;
import zipkin2.junit.ZipkinRule;
import zipkin2.storage.InMemoryStorage;

public class ZipkinTest {
    private static boolean IS_BOOTED = false;
    public static ZipkinRule ZIPKIN_SERVER = new ZipkinRule();

    @Before
    public void setup() throws IOException {
        if (!IS_BOOTED) {
            ZIPKIN_SERVER.start(9091);
        }
        Config.Collector.BACKEND_SERVICE = ZIPKIN_SERVER.httpUrl() + "/api/v2/spans";
        Config.Agent.SERVICE_NAME = "Zipkin-Core";
        if (!IS_BOOTED) {
            ServiceManager.INSTANCE.boot();
            IS_BOOTED = true;
        }
    }

    @After
    public void clean() {
        ((InMemoryStorage) Whitebox.getInternalState(ZIPKIN_SERVER, "storage")).clear();
        Config.Collector.BACKEND_SERVICE = null;
        Config.Agent.SERVICE_NAME = null;
    }

    /**
     * @param time * 2s = Max wait time
     * @return traces
     */
    protected List<List<Span>> readTracesUntilTimeout(int time, int expectedTraceSize, int expectedSpanSize) {
        List<List<Span>> traces = null;
        boolean received = false;
        for (int i = time; i >= 0; i--) {
            try {
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
            }
            traces = ZIPKIN_SERVER.getTraces();
            if (traces.size() == expectedTraceSize) {
                int numOfSpan = 0;
                for (final List<Span> spans : traces) {
                    numOfSpan += spans.size();
                }
                if (numOfSpan == expectedSpanSize) {
                    received = true;
                    break;
                }
            }
        }

        Assert.assertTrue(received);
        return traces;
    }
}
