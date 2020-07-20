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

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.apache.skywalking.apm.agent.core.context.ids.DistributedTraceId;

/**
 * The <code>ContextSnapshot</code> is a snapshot for current context. The snapshot carries the info for building
 * reference between two segments in two thread, but have a causal relationship.
 */
public class ContextSnapshot {
    @Getter
    @Setter
    private PrimaryContextSnapshot primaryContextSnapshot;
    @Getter
    private CorrelationContext correlationContext;
    @Getter
    private ExtensionContext extensionContext;

    /**
     * Additional key:value(s) for propagation.
     *
     * <p>These context should never be used on the core and plugin codes.</p>
     *
     * Only highly customized core extension, such as new tracer or new tracer context should use this to re-use agent
     * propagation mechanism.
     */
    private Map<String, Object> customContext;

    /**
     * Create standard ContextSnapshot for SkyWalking default core.
     */
    public ContextSnapshot(String traceSegmentId,
                           int spanId,
                           DistributedTraceId primaryTraceId,
                           String parentEndpoint,
                           CorrelationContext correlationContext,
                           ExtensionContext extensionContext) {
        this.primaryContextSnapshot =
            new PrimaryContextSnapshot(primaryTraceId, traceSegmentId, spanId, parentEndpoint);
        this.correlationContext = correlationContext.clone();
        this.extensionContext = extensionContext.clone();
    }

    /**
     * Create an empty ContextSnapshot shell, for extension only.
     */
    public ContextSnapshot(CorrelationContext correlationContext) {
        this.correlationContext = correlationContext.clone();
    }

    public boolean isFromCurrent() {
        return primaryContextSnapshot.isFromCurrent();
    }

    public CorrelationContext getCorrelationContext() {
        return correlationContext;
    }

    public boolean isValid() {
        return primaryContextSnapshot.isValid();
    }

    /**
     * Add custom key:value pair to propagate. Only work in the capture stage.
     */
    public void addCustomContext(String key, Object value) {
        if (customContext == null) {
            customContext = new HashMap<>();
        }
        customContext.put(key, value);
    }

    /**
     * Read cached propagated context.
     */
    public Object readCustomContext(String key) {
        if (customContext == null) {
            return null;
        } else {
            return customContext.get(key);
        }
    }
}
