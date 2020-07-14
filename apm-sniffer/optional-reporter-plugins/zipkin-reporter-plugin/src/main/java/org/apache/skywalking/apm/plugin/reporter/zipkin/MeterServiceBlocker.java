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
import org.apache.skywalking.apm.agent.core.meter.MeterService;
import org.apache.skywalking.apm.agent.core.meter.transform.MeterTransformer;

/**
 * MeterServiceBlocker blocks the original implementation.
 */
@OverrideImplementor(MeterService.class)
public class MeterServiceBlocker extends MeterService {
    @Override
    public <T extends MeterTransformer> void register(final T meterTransformer) {
    }

    @Override
    public void prepare() throws Throwable {
    }

    @Override
    public void boot() throws Throwable {
    }

    @Override
    public void shutdown() throws Throwable {
    }
}
