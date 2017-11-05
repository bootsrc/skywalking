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

package org.skywalking.apm.collector.storage.es.dao;

import java.util.HashMap;
import java.util.Map;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.skywalking.apm.collector.storage.base.dao.IPersistenceDAO;
import org.skywalking.apm.collector.storage.dao.IGCMetricDAO;
import org.skywalking.apm.collector.storage.es.base.dao.EsDAO;
import org.skywalking.apm.collector.storage.table.jvm.GCMetric;
import org.skywalking.apm.collector.storage.table.jvm.GCMetricTable;

/**
 * @author peng-yongsheng
 */
public class GCMetricEsDAO extends EsDAO implements IGCMetricDAO, IPersistenceDAO<IndexRequestBuilder, UpdateRequestBuilder, GCMetric> {

    @Override public GCMetric get(String id) {
        return null;
    }

    @Override public IndexRequestBuilder prepareBatchInsert(GCMetric gcMetric) {
        Map<String, Object> source = new HashMap<>();
        source.put(GCMetricTable.COLUMN_INSTANCE_ID, gcMetric.getInstanceId());
        source.put(GCMetricTable.COLUMN_PHRASE, gcMetric.getPhrase());
        source.put(GCMetricTable.COLUMN_COUNT, gcMetric.getCount());
        source.put(GCMetricTable.COLUMN_TIME, gcMetric.getTime());
        source.put(GCMetricTable.COLUMN_TIME_BUCKET, gcMetric.getTimeBucket());

        return getClient().prepareIndex(GCMetricTable.TABLE, gcMetric.getId()).setSource(source);
    }

    @Override public UpdateRequestBuilder prepareBatchUpdate(GCMetric gcMetric) {
        return null;
    }
}
