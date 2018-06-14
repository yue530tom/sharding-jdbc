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

package io.shardingsphere.core.jdbc.core.connection;

import com.google.common.util.concurrent.ListeningExecutorService;
import io.shardingsphere.core.api.config.MasterSlaveRuleConfiguration;
import io.shardingsphere.core.api.config.ShardingRuleConfiguration;
import io.shardingsphere.core.api.config.TableRuleConfiguration;
import io.shardingsphere.core.fixture.TestDataSource;
import io.shardingsphere.core.jdbc.core.ShardingContext;
import io.shardingsphere.core.jdbc.core.datasource.MasterSlaveDataSource;
import io.shardingsphere.core.jdbc.metadata.JDBCShardingMetaData;
import io.shardingsphere.core.metadata.ShardingMetaData;
import io.shardingsphere.core.rule.ShardingRule;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public final class ShardingConnectionTest {
    
    private static MasterSlaveDataSource masterSlaveDataSource;
    
    private static final String DS_NAME = "default";
    
    private ShardingConnection connection;
    
    @BeforeClass
    public static void init() throws SQLException {
        DataSource masterDataSource = new TestDataSource("test_ds_master");
        DataSource slaveDataSource = new TestDataSource("test_ds_slave");
        Map<String, DataSource> dataSourceMap = new HashMap<>(2, 1);
        dataSourceMap.put("test_ds_master", masterDataSource);
        dataSourceMap.put("test_ds_slave", slaveDataSource);
        masterSlaveDataSource = new MasterSlaveDataSource(
                dataSourceMap, new MasterSlaveRuleConfiguration("test_ds", "test_ds_master", Collections.singletonList("test_ds_slave")), Collections.<String, Object>emptyMap());
        ((TestDataSource) slaveDataSource).setThrowExceptionWhenClosing(true);
    }
    
    @Before
    public void setUp() {
        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();
        TableRuleConfiguration tableRuleConfig = new TableRuleConfiguration();
        tableRuleConfig.setLogicTable("test");
        shardingRuleConfig.getTableRuleConfigs().add(tableRuleConfig);
        Map<String, DataSource> dataSourceMap = new HashMap<>(1, 1);
        dataSourceMap.put(DS_NAME, masterSlaveDataSource);
        ShardingRule shardingRule = new ShardingRule(shardingRuleConfig, dataSourceMap.keySet());
        ListeningExecutorService executorService = Mockito.mock(ListeningExecutorService.class);
        ShardingMetaData shardingMetaData = new JDBCShardingMetaData(executorService, dataSourceMap, shardingRule, null);
        ShardingContext shardingContext = new ShardingContext(dataSourceMap, shardingRule, null, null, shardingMetaData, false);
        connection = new ShardingConnection(shardingContext);
    }
    
    @After
    public void clear() {
        try {
            connection.close();
        } catch (final SQLException ignore) {
        }
    }
    
    @Test
    public void assertGetConnectionFromCache() throws Exception {
        assertSame(connection.getConnection(DS_NAME), connection.getConnection(DS_NAME));
    }
    
    @Test(expected = IllegalStateException.class)
    public void assertGetConnectionFailure() throws Exception {
        connection.getConnection("not_exist");
    }
    
    @Test
    public void assertRelease() throws Exception {
        Connection conn = connection.getConnection(DS_NAME);
        connection.release(conn);
        assertNotSame(conn, connection.getConnection(DS_NAME));
    }
}
