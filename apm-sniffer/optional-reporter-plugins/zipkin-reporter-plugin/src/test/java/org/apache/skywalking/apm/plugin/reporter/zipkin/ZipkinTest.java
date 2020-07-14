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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import zipkin2.Span;
import zipkin2.junit.ZipkinRule;

public class ZipkinTest {
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

    /**
     * @param time * 2s = Max wait time
     * @return traces
     */
    protected List<List<Span>> readTracesUntilTimeout(int time) {
        List<List<Span>> traces = null;
        boolean received = false;
        for (int i = time; i >= 0; i--) {
            try {
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
            }
            traces = zipkin.getTraces();
            if (traces.size() > 0) {
                received = true;
                break;
            }
        }

        Assert.assertTrue(received);
        return traces;
    }
}
