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

package org.apache.skywalking.apm.collector.storage.h2.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import org.apache.skywalking.apm.collector.client.h2.H2Client;
import org.apache.skywalking.apm.collector.client.h2.H2ClientException;
import org.apache.skywalking.apm.collector.core.util.Const;
import org.apache.skywalking.apm.collector.storage.base.sql.SqlBuilder;
import org.apache.skywalking.apm.collector.storage.dao.IInstanceMetricUIDAO;
import org.apache.skywalking.apm.collector.storage.h2.base.dao.H2DAO;
import org.apache.skywalking.apm.collector.storage.table.MetricSource;
import org.apache.skywalking.apm.collector.storage.table.instance.InstanceMetricTable;
import org.apache.skywalking.apm.collector.storage.ui.common.Step;
import org.apache.skywalking.apm.collector.storage.utils.DurationPoint;
import org.apache.skywalking.apm.collector.storage.utils.TimePyramidTableNameBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author peng-yongsheng, clevertension
 */
public class InstanceMetricH2UIDAO extends H2DAO implements IInstanceMetricUIDAO {

    private final Logger logger = LoggerFactory.getLogger(InstanceMetricH2UIDAO.class);
    private static final String GET_INSTANCE_METRIC_SQL = "select * from {0} where {1} = ? and {2} in (";
    private static final String GET_TPS_METRIC_SQL = "select * from {0} where {1} = ?";

    public InstanceMetricH2UIDAO(H2Client client) {
        super(client);
    }

    @Override public InstanceMetric get(long[] timeBuckets, int instanceId) {
        H2Client client = getClient();
        logger.info("the inst performance inst id = {}", instanceId);
        String sql = SqlBuilder.buildSql(GET_INSTANCE_METRIC_SQL, InstanceMetricTable.TABLE, InstanceMetricTable.COLUMN_INSTANCE_ID, InstanceMetricTable.COLUMN_TIME_BUCKET);
        StringBuilder builder = new StringBuilder();
        for (long timeBucket : timeBuckets) {
            builder.append("?,");
        }
        builder.delete(builder.length() - 1, builder.length());
        builder.append(")");
        sql = sql + builder;
        Object[] params = new Object[timeBuckets.length + 1];
        for (int i = 0; i < timeBuckets.length; i++) {
            params[i + 1] = timeBuckets[i];
        }
        params[0] = instanceId;
        try (ResultSet rs = client.executeQuery(sql, params)) {
            if (rs.next()) {
                long callTimes = rs.getInt(InstanceMetricTable.COLUMN_TRANSACTION_CALLS);
                long costTotal = rs.getInt(InstanceMetricTable.COLUMN_TRANSACTION_DURATION_SUM);
                return new InstanceMetric(instanceId, callTimes, costTotal);
            }
        } catch (SQLException | H2ClientException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override public long getTpsMetric(int instanceId, long timeBucket) {
        logger.info("getTpMetric instanceId = {}, startTimeBucket = {}", instanceId, timeBucket);
        H2Client client = getClient();
        String sql = SqlBuilder.buildSql(GET_TPS_METRIC_SQL, InstanceMetricTable.TABLE, InstanceMetricTable.COLUMN_ID);
        Object[] params = new Object[] {instanceId};
        try (ResultSet rs = client.executeQuery(sql, params)) {
            if (rs.next()) {
                return rs.getLong(InstanceMetricTable.COLUMN_TRANSACTION_CALLS);
            }
        } catch (SQLException | H2ClientException e) {
            logger.error(e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public List<Integer> getServerTPSTrend(int instanceId, Step step, List<DurationPoint> durationPoints) {
        H2Client client = getClient();
        String tableName = TimePyramidTableNameBuilder.build(step, InstanceMetricTable.TABLE);

        String sql = SqlBuilder.buildSql(GET_TPS_METRIC_SQL, tableName, InstanceMetricTable.COLUMN_ID);

        List<Integer> throughputTrend = new LinkedList<>();
        durationPoints.forEach(durationPoint -> {
            String id = durationPoint.getPoint() + Const.ID_SPLIT + instanceId + Const.ID_SPLIT + MetricSource.Callee.getValue();
            try (ResultSet rs = client.executeQuery(sql, new Object[] {id})) {
                if (rs.next()) {
                    long callTimes = rs.getLong(InstanceMetricTable.COLUMN_TRANSACTION_CALLS);
                    throughputTrend.add((int)(callTimes / durationPoint.getSecondsBetween()));
                } else {
                    throughputTrend.add(0);
                }
            } catch (SQLException | H2ClientException e) {
                logger.error(e.getMessage(), e);
            }
        });

        return throughputTrend;
    }

    @Override public long getRespTimeMetric(int instanceId, long timeBucket) {
        H2Client client = getClient();
        String sql = SqlBuilder.buildSql(GET_TPS_METRIC_SQL, InstanceMetricTable.TABLE, InstanceMetricTable.COLUMN_ID);
        Object[] params = new Object[] {instanceId};
        try (ResultSet rs = client.executeQuery(sql, params)) {
            if (rs.next()) {
                long callTimes = rs.getLong(InstanceMetricTable.COLUMN_TRANSACTION_CALLS);
                long costTotal = rs.getLong(InstanceMetricTable.COLUMN_TRANSACTION_DURATION_SUM);
                return costTotal / callTimes;
            }
        } catch (SQLException | H2ClientException e) {
            logger.error(e.getMessage(), e);
        }
        return 0;
    }

    @Override public List<Integer> getResponseTimeTrend(int instanceId, Step step, List<DurationPoint> durationPoints) {
        H2Client client = getClient();

        String tableName = TimePyramidTableNameBuilder.build(step, InstanceMetricTable.TABLE);
        String sql = SqlBuilder.buildSql(GET_TPS_METRIC_SQL, tableName, InstanceMetricTable.COLUMN_ID);

        List<Integer> responseTimeTrends = new LinkedList<>();
        durationPoints.forEach(durationPoint -> {
            String id = durationPoint.getPoint() + Const.ID_SPLIT + instanceId + Const.ID_SPLIT + MetricSource.Callee.getValue();
            try (ResultSet rs = client.executeQuery(sql, new Object[] {id})) {
                if (rs.next()) {
                    long callTimes = rs.getLong(InstanceMetricTable.COLUMN_TRANSACTION_CALLS);
                    long errorCallTimes = rs.getLong(InstanceMetricTable.COLUMN_TRANSACTION_ERROR_CALLS);
                    long durationSum = rs.getLong(InstanceMetricTable.COLUMN_TRANSACTION_DURATION_SUM);
                    long errorDurationSum = rs.getLong(InstanceMetricTable.COLUMN_BUSINESS_TRANSACTION_ERROR_DURATION_SUM);
                    responseTimeTrends.add((int)((durationSum - errorDurationSum) / (callTimes - errorCallTimes)));
                } else {
                    responseTimeTrends.add(0);
                }
            } catch (SQLException | H2ClientException e) {
                logger.error(e.getMessage(), e);
            }
        });
        return responseTimeTrends;
    }
}
