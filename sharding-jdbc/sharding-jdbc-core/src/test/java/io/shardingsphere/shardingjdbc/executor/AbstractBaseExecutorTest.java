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

package io.shardingsphere.shardingjdbc.executor;

import io.shardingsphere.core.constant.DatabaseType;
import io.shardingsphere.core.event.ShardingEventBusInstance;
import io.shardingsphere.core.executor.ShardingExecuteEngine;
import io.shardingsphere.core.executor.sql.execute.threadlocal.ExecutorExceptionHandler;
import io.shardingsphere.shardingjdbc.executor.fixture.EventCaller;
import io.shardingsphere.shardingjdbc.executor.fixture.ExecutorTestUtil;
import io.shardingsphere.shardingjdbc.executor.fixture.TestDMLExecutionEventListener;
import io.shardingsphere.shardingjdbc.executor.fixture.TestDQLExecutionEventListener;
import io.shardingsphere.shardingjdbc.jdbc.core.ShardingContext;
import io.shardingsphere.shardingjdbc.jdbc.core.connection.ShardingConnection;
import lombok.AccessLevel;
import lombok.Getter;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Getter(AccessLevel.PROTECTED)
public abstract class AbstractBaseExecutorTest {
    
    private ShardingExecuteEngine executeEngine;
    
    private ShardingConnection connection;
    
    @Mock
    private EventCaller eventCaller;
    
    private TestDQLExecutionEventListener dqlExecutionEventListener;
    
    private TestDMLExecutionEventListener dmlExecutionEventListener;
    
    @Before
    public void setUp() throws SQLException {
        MockitoAnnotations.initMocks(this);
        ExecutorExceptionHandler.setExceptionThrown(false);
        executeEngine = new ShardingExecuteEngine(Runtime.getRuntime().availableProcessors());
        dqlExecutionEventListener = new TestDQLExecutionEventListener(eventCaller);
        dmlExecutionEventListener = new TestDMLExecutionEventListener(eventCaller);
        setConnection();
        register();
    }
    
    private void register() {
        ShardingEventBusInstance.getInstance().register(dqlExecutionEventListener);
        ShardingEventBusInstance.getInstance().register(dmlExecutionEventListener);
    }
    
    private void setConnection() throws SQLException {
        ShardingContext shardingContext = mock(ShardingContext.class);
        when(shardingContext.getExecuteEngine()).thenReturn(executeEngine);
        when(shardingContext.getMaxConnectionsSizePerQuery()).thenReturn(1);
        when(shardingContext.getDatabaseType()).thenReturn(DatabaseType.H2);
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(mock(Connection.class));
        Map<String, DataSource> dataSourceSourceMap = new LinkedHashMap<>();
        dataSourceSourceMap.put("ds_0", dataSource);
        dataSourceSourceMap.put("ds_1", dataSource);
        connection = new ShardingConnection(dataSourceSourceMap, shardingContext);
    }
    
    @After
    public void tearDown() {
        ExecutorTestUtil.clear();
        ShardingEventBusInstance.getInstance().unregister(dqlExecutionEventListener);
        ShardingEventBusInstance.getInstance().unregister(dmlExecutionEventListener);
        executeEngine.close();
    }
}
