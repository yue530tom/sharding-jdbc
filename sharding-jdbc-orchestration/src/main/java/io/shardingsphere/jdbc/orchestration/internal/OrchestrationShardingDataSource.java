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

package io.shardingsphere.jdbc.orchestration.internal;

import io.shardingsphere.core.api.config.ShardingRuleConfiguration;
import io.shardingsphere.core.jdbc.core.datasource.ShardingDataSource;
import io.shardingsphere.core.rule.ShardingRule;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

/**
 * Orchestration sharding datasource.
 *
 * @author caohao
 */
@Slf4j
public class OrchestrationShardingDataSource extends ShardingDataSource {
    
    private final OrchestrationFacade orchestrationFacade;
    
    private final Map<String, DataSource> dataSourceMap;
    
    private final ShardingRuleConfiguration shardingRuleConfig;
    
    private final Map<String, Object> configMap;
    
    private final Properties props;
    
    public OrchestrationShardingDataSource(final Map<String, DataSource> dataSourceMap, final ShardingRuleConfiguration shardingRuleConfig,
                                           final Map<String, Object> configMap, final Properties props, final OrchestrationFacade orchestrationFacade) throws SQLException {
        super(getRawDataSourceMap(dataSourceMap), new ShardingRule(getShardingRuleConfiguration(dataSourceMap, shardingRuleConfig), getRawDataSourceMap(dataSourceMap).keySet()), configMap, props);
        this.orchestrationFacade = orchestrationFacade;
        this.dataSourceMap = getRawDataSourceMap(dataSourceMap);
        this.shardingRuleConfig = getShardingRuleConfiguration(dataSourceMap, shardingRuleConfig);
        this.configMap = configMap;
        this.props = props;
    }
    
    /**
     * Initialize for sharding orchestration.
     */
    public void init() {
        orchestrationFacade.init(dataSourceMap, shardingRuleConfig, configMap, props, this);
    }
    
    @Override
    public void close() {
        super.close();
        orchestrationFacade.close();
    }
}
