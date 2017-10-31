/*
 * Copyright 2017, OpenSkywalking Organization All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project repository: https://github.com/OpenSkywalking/skywalking
 */

package org.skywalking.apm.plugin.jdbc.mysql.define;

import org.skywalking.apm.agent.core.plugin.match.ClassMatch;

import static org.skywalking.apm.agent.core.plugin.match.NameMatch.byName;

/**
 * {@link JDBC42PreparedStatementInstrumentation} intercepts {@link com.mysql.jdbc.JDBC42PreparedStatement} class.
 *
 * @author zhangxin
 */
public class JDBC42PreparedStatementInstrumentation extends AbstractPreparedStatementInstrumentation {
    private static final String ENHANCE_CLASS = "com.mysql.jdbc.JDBC42PreparedStatement";

    @Override protected ClassMatch enhanceClass() {
        return byName(ENHANCE_CLASS);
    }
}
