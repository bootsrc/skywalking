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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 * {@link ContextCarrier} is a data carrier of {@link TracingContext}. It holds the snapshot (current state) of {@link
 * TracingContext}.
 * <p>
 */
public class ContextCarrier implements Serializable {
    @Getter
    private PrimaryContext primaryContext = new PrimaryContext();
    @Getter
    private CorrelationContext correlationContext = new CorrelationContext();
    @Getter
    private ExtensionContext extensionContext = new ExtensionContext();

    /**
     * Additional keys for reading propagated context.
     *
     * <p>These context should never be used on the core and plugin codes.</p>
     *
     * Only highly customized core extension, such as new tracer or new tracer context should use this to re-use agent
     * propagation mechanism.
     */
    private String[] customKeys;
    /**
     * Additional key:value(s) for propagation.
     *
     * <p>These context should never be used on the core and plugin codes.</p>
     *
     * Only highly customized core extension, such as new tracer or new tracer context should use this to re-use agent
     * propagation mechanism.
     */
    private Map<String, String> customContext;

    /**
     * @return items required to propagate
     */
    public CarrierItem items() {
        CarrierItem customItemsHead = null;
        if (customContext != null) {
            for (final Map.Entry<String, String> keyValuePair : customContext.entrySet()) {
                customItemsHead = new CarrierItem(keyValuePair.getKey(), keyValuePair.getValue(), customItemsHead);
            }
        } else {
            if (customKeys != null) {
                for (final String customKey : customKeys) {
                    customItemsHead = new CarrierItem(customKey, "", customItemsHead);
                }
            }
        }
        SW8ExtensionCarrierItem sw8ExtensionCarrierItem = new SW8ExtensionCarrierItem(
            extensionContext, customItemsHead);
        SW8CorrelationCarrierItem sw8CorrelationCarrierItem = new SW8CorrelationCarrierItem(
            correlationContext, sw8ExtensionCarrierItem);
        SW8CarrierItem sw8CarrierItem = new SW8CarrierItem(primaryContext, sw8CorrelationCarrierItem);
        return new CarrierItemHead(sw8CarrierItem);
    }

    /**
     * Add custom key:value pair to propagate. Only work before the injection.
     */
    public void addCustomContext(String key, String value) {
        if (customContext == null) {
            customContext = new HashMap<>();
        }
        customContext.put(key, value);
    }

    /**
     * Read propagated context. The key should be set through {@link #setCustomKeys(String...)} before read.
     */
    public String readCustomContext(String key) {
        if (customContext == null) {
            return null;
        } else {
            return customContext.get(key);
        }
    }

    /**
     * @return true if SkyWalking primary context is valid.
     */
    public boolean isValid() {
        return primaryContext.isValid();
    }

    /**
     * Add custom key(s) to read from propagated context(usually headers or metadata of RPC).
     */
    public void setCustomKeys(final String... customKeys) {
        this.customKeys = customKeys;
    }
}
