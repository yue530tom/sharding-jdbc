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

package io.shardingsphere.shardingjdbc.jdbc.core.datasource;

import io.shardingsphere.api.ConfigMapContext;
import io.shardingsphere.api.config.MasterSlaveRuleConfiguration;
import io.shardingsphere.core.constant.properties.ShardingProperties;
import io.shardingsphere.core.constant.properties.ShardingPropertiesConstant;
import io.shardingsphere.core.rule.MasterSlaveRule;
import io.shardingsphere.shardingjdbc.jdbc.adapter.AbstractDataSourceAdapter;
import io.shardingsphere.shardingjdbc.jdbc.core.connection.MasterSlaveConnection;
import lombok.Getter;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Database that support master-slave.
 *
 * @author zhangliang
 * @author panjuan
 */
@Getter
public class MasterSlaveDataSource extends AbstractDataSourceAdapter {
    
    private final MasterSlaveRule masterSlaveRule;
    
    private final ShardingProperties shardingProperties;
    
    public MasterSlaveDataSource(final Map<String, DataSource> dataSourceMap, final MasterSlaveRuleConfiguration masterSlaveRuleConfig,
                                 final Map<String, Object> configMap, final Properties props) throws SQLException {
        super(dataSourceMap);
        if (!configMap.isEmpty()) {
            ConfigMapContext.getInstance().getConfigMap().putAll(configMap);
        }
        this.masterSlaveRule = new MasterSlaveRule(masterSlaveRuleConfig);
        shardingProperties = new ShardingProperties(null == props ? new Properties() : props);
    }
    
    public MasterSlaveDataSource(final Map<String, DataSource> dataSourceMap, final MasterSlaveRule masterSlaveRule,
                                 final Map<String, Object> configMap, final Properties props) throws SQLException {
        super(dataSourceMap);
        if (!configMap.isEmpty()) {
            ConfigMapContext.getInstance().getConfigMap().putAll(configMap);
        }
        this.masterSlaveRule = masterSlaveRule;
        shardingProperties = new ShardingProperties(null == props ? new Properties() : props);
    }
    
    /**
     * Get map of all actual data source name and all actual data sources.
     *
     * @return map of all actual data source name and all actual data sources
     */
    public Map<String, DataSource> getAllDataSources() {
        Map<String, DataSource> result = new HashMap<>(masterSlaveRule.getSlaveDataSourceNames().size() + 1, 1);
        result.put(masterSlaveRule.getMasterDataSourceName(), getDataSourceMap().get(masterSlaveRule.getMasterDataSourceName()));
        for (String each : masterSlaveRule.getSlaveDataSourceNames()) {
            result.put(each, getDataSourceMap().get(each));
        }
        return result;
    }
    
    @Override
    public final MasterSlaveConnection getConnection() {
        return new MasterSlaveConnection(this);
    }
    
    /**
     * Show SQL or not.
     *
     * @return show SQL or not
     */
    public boolean showSQL() {
        return shardingProperties.getValue(ShardingPropertiesConstant.SQL_SHOW);
    }
}

