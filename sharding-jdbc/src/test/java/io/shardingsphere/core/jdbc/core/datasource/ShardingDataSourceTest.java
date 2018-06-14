/*
 * Copyright 2016-2018 shardingsphere.io.
 * <p>
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
 * </p>
 */

package io.shardingsphere.core.jdbc.core.datasource;

import com.google.common.base.Joiner;
import io.shardingsphere.core.api.MasterSlaveDataSourceFactory;
import io.shardingsphere.core.api.config.MasterSlaveRuleConfiguration;
import io.shardingsphere.core.api.config.ShardingRuleConfiguration;
import io.shardingsphere.core.api.config.TableRuleConfiguration;
import io.shardingsphere.core.constant.ShardingPropertiesConstant;
import io.shardingsphere.core.executor.ExecutorEngine;
import io.shardingsphere.core.rule.ShardingRule;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public final class ShardingDataSourceTest {
    
    @Test(expected = IllegalStateException.class)
    public void assertGetDatabaseProductNameWhenDataBaseProductNameDifferent() throws SQLException {
        DataSource dataSource1 = mockDataSource("MySQL");
        DataSource dataSource2 = mockDataSource("H2");
        Map<String, DataSource> dataSourceMap = new HashMap<>(2, 1);
        dataSourceMap.put("ds1", dataSource1);
        dataSourceMap.put("ds2", dataSource2);
        assertDatabaseProductName(dataSourceMap, dataSource1.getConnection(), dataSource2.getConnection());
    }
    
    @Test(expected = IllegalStateException.class)
    public void assertGetDatabaseProductNameWhenDataBaseProductNameDifferentForMasterSlave() throws SQLException {
        DataSource dataSource1 = mockDataSource("MySQL");
        DataSource masterDataSource = mockDataSource("H2");
        DataSource slaveDataSource = mockDataSource("H2");
        Map<String, DataSource> masterSlaveDataSourceMap = new HashMap<>(2, 1);
        masterSlaveDataSourceMap.put("masterDataSource", masterDataSource);
        masterSlaveDataSourceMap.put("slaveDataSource", slaveDataSource);
        MasterSlaveDataSource dataSource2 = (MasterSlaveDataSource) MasterSlaveDataSourceFactory.createDataSource(
                masterSlaveDataSourceMap, new MasterSlaveRuleConfiguration("ds", "masterDataSource", Collections.singletonList("slaveDataSource")), Collections.<String, Object>emptyMap());
        Map<String, DataSource> dataSourceMap = new HashMap<>(2, 1);
        dataSourceMap.put("ds1", dataSource1);
        dataSourceMap.put("ds2", dataSource2);
        assertDatabaseProductName(dataSourceMap, dataSource1.getConnection(), 
                dataSource2.getDataSourceMap().get("masterDataSource").getConnection(), dataSource2.getDataSourceMap().get("slaveDataSource").getConnection());
    }
    
    @Test
    public void assertGetDatabaseProductName() throws SQLException {
        DataSource dataSource1 = mockDataSource("H2");
        DataSource dataSource2 = mockDataSource("H2");
        DataSource dataSource3 = mockDataSource("H2");
        Map<String, DataSource> dataSourceMap = new HashMap<>(3, 1);
        dataSourceMap.put("ds1", dataSource1);
        dataSourceMap.put("ds2", dataSource2);
        dataSourceMap.put("ds3", dataSource3);
        assertDatabaseProductName(dataSourceMap, dataSource1.getConnection(), dataSource2.getConnection(), dataSource3.getConnection());
    }
    
    @Test
    public void assertGetDatabaseProductNameForMasterSlave() throws SQLException {
        DataSource dataSource1 = mockDataSource("H2");
        DataSource masterDataSource = mockDataSource("H2");
        DataSource slaveDataSource = mockDataSource("H2");
        DataSource dataSource3 = mockDataSource("H2");
        Map<String, DataSource> dataSourceMap = new HashMap<>(4, 1);
        dataSourceMap.put("ds1", dataSource1);
        dataSourceMap.put("masterDataSource", masterDataSource);
        dataSourceMap.put("slaveDataSource", slaveDataSource);
        dataSourceMap.put("ds3", dataSource3);
        assertDatabaseProductName(dataSourceMap, dataSource1.getConnection(), masterDataSource.getConnection(), slaveDataSource.getConnection());
    }
    
    private void assertDatabaseProductName(final Map<String, DataSource> dataSourceMap, final Connection... connections) throws SQLException {
        try {
            createShardingDataSource(dataSourceMap).getDatabaseType();
        } finally {
            for (Connection each : connections) {
                verify(each, atLeast(1)).close();
            }
        }
    }
    
    private DataSource mockDataSource(final String dataBaseProductName) throws SQLException {
        DataSource result = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(false);
        when(statement.getResultSet()).thenReturn(resultSet);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn(dataBaseProductName);
        when(result.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(ArgumentMatchers.<String>any())).thenReturn(resultSet);
        when(statement.getConnection()).thenReturn(connection);
        when(statement.getConnection().getMetaData().getTables(ArgumentMatchers.<String>any(), ArgumentMatchers.<String>any(),
                ArgumentMatchers.<String>any(), ArgumentMatchers.<String[]>any())).thenReturn(resultSet);
        return result;
    }
    
    @Test
    public void assertGetConnection() throws SQLException {
        DataSource dataSource = mockDataSource("H2");
        Map<String, DataSource> dataSourceMap = new HashMap<>(1, 1);
        dataSourceMap.put("ds", dataSource);
        assertThat(createShardingDataSource(dataSourceMap).getConnection().getConnection("ds"), is(dataSource.getConnection()));
    }
    
    @Test
    public void assertRenewWithoutChangeExecutorPoolEngine() throws SQLException, NoSuchFieldException, IllegalAccessException {
        DataSource originalDataSource = mockDataSource("H2");
        Map<String, DataSource> originalDataSourceMap = new HashMap<>(1, 1);
        originalDataSourceMap.put("ds", originalDataSource);
        ShardingDataSource shardingDataSource = createShardingDataSource(originalDataSourceMap);
        ExecutorEngine originExecutorEngine = getExecutorEngine(shardingDataSource);
        DataSource newDataSource = mockDataSource("H2");
        Map<String, DataSource> newDataSourceMap = new HashMap<>(1, 1);
        newDataSourceMap.put("ds", newDataSource);
        shardingDataSource.renew(newDataSourceMap, new ShardingRule(createShardingRuleConfig(newDataSourceMap), newDataSourceMap.keySet()), new Properties());
        assertThat(originExecutorEngine, is(getExecutorEngine(shardingDataSource)));
    }
    
    @Test
    public void assertRenewWithChangeExecutorEnginePoolSize() throws SQLException, NoSuchFieldException, IllegalAccessException {
        DataSource originalDataSource = mockDataSource("H2");
        Map<String, DataSource> originalDataSourceMap = new HashMap<>(1, 1);
        originalDataSourceMap.put("ds", originalDataSource);
        ShardingDataSource shardingDataSource = createShardingDataSource(originalDataSourceMap);
        final ExecutorEngine originExecutorEngine = getExecutorEngine(shardingDataSource);
        DataSource newDataSource = mockDataSource("H2");
        Map<String, DataSource> newDataSourceMap = new HashMap<>(1, 1);
        newDataSourceMap.put("ds", newDataSource);
        Properties props = new Properties();
        props.setProperty(ShardingPropertiesConstant.EXECUTOR_SIZE.getKey(), "100");
        shardingDataSource.renew(newDataSourceMap, new ShardingRule(createShardingRuleConfig(newDataSourceMap), newDataSourceMap.keySet()), props);
        assertThat(originExecutorEngine, not(getExecutorEngine(shardingDataSource)));
    }
    
    // TODO to be discuss
    // @Test(expected = IllegalStateException.class)
    @Test
    public void assertRenewWithDatabaseTypeChanged() throws SQLException {
        DataSource originalDataSource = mockDataSource("H2");
        Map<String, DataSource> originalDataSourceMap = new HashMap<>(1, 1);
        originalDataSourceMap.put("ds", originalDataSource);
        ShardingDataSource shardingDataSource = createShardingDataSource(originalDataSourceMap);
        DataSource newDataSource = mockDataSource("MySQL");
        Map<String, DataSource> newDataSourceMap = new HashMap<>(1, 1);
        newDataSourceMap.put("ds", newDataSource);
        shardingDataSource.renew(newDataSourceMap, new ShardingRule(createShardingRuleConfig(newDataSourceMap), newDataSourceMap.keySet()), new Properties());
    }
    
    private ShardingDataSource createShardingDataSource(final Map<String, DataSource> dataSourceMap) throws SQLException {
        return new ShardingDataSource(dataSourceMap, new ShardingRule(createShardingRuleConfig(dataSourceMap), dataSourceMap.keySet()));
    }
    
    private ShardingRuleConfiguration createShardingRuleConfig(final Map<String, DataSource> dataSourceMap) {
        final ShardingRuleConfiguration result = new ShardingRuleConfiguration();
        TableRuleConfiguration tableRuleConfig = new TableRuleConfiguration();
        tableRuleConfig.setLogicTable("logicTable");
        List<String> orderActualDataNodes = new LinkedList<>();
        for (String each : dataSourceMap.keySet()) {
            orderActualDataNodes.add(each + ".table_${0..2}");
        }
        tableRuleConfig.setActualDataNodes(Joiner.on(",").join(orderActualDataNodes));
        result.getTableRuleConfigs().add(tableRuleConfig);
        return result;
    }
    
    private ExecutorEngine getExecutorEngine(final ShardingDataSource shardingDataSource) throws NoSuchFieldException, IllegalAccessException {
        Field field = ShardingDataSource.class.getDeclaredField("executorEngine");
        field.setAccessible(true);
        return (ExecutorEngine) field.get(shardingDataSource);
    }
}
