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

package org.apache.skywalking.apm.agent.core.context;

import lombok.Getter;
import org.apache.skywalking.apm.agent.core.context.ids.DistributedTraceId;

@Getter
public class PrimaryContextSnapshot {
    private DistributedTraceId traceId;
    private String traceSegmentId;
    private int spanId;
    private String parentEndpoint;

    public PrimaryContextSnapshot(final DistributedTraceId traceId,
                                  final String traceSegmentId,
                                  final int spanId,
                                  final String parentEndpoint) {
        this.traceId = traceId;
        this.traceSegmentId = traceSegmentId;
        this.spanId = spanId;
        this.parentEndpoint = parentEndpoint;
    }

    public boolean isFromCurrent() {
        return traceSegmentId != null
            && traceSegmentId.equals(ContextManager.capture().getPrimaryContextSnapshot().getTraceSegmentId());
    }

    public boolean isValid() {
        return traceSegmentId != null && spanId > -1 && traceId != null;
    }
}
