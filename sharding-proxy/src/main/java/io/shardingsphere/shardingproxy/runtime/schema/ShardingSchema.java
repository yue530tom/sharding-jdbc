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

package io.shardingsphere.shardingproxy.runtime.schema;

import com.google.common.eventbus.Subscribe;
import io.shardingsphere.api.config.ShardingRuleConfiguration;
import io.shardingsphere.core.constant.ShardingConstant;
import io.shardingsphere.core.rule.DataSourceParameter;
import io.shardingsphere.core.rule.MasterSlaveRule;
import io.shardingsphere.core.rule.ShardingRule;
import io.shardingsphere.orchestration.internal.event.config.ShardingRuleChangedEvent;
import io.shardingsphere.orchestration.internal.event.state.DisabledStateEventBusEvent;
import io.shardingsphere.orchestration.internal.rule.OrchestrationMasterSlaveRule;
import io.shardingsphere.orchestration.internal.rule.OrchestrationShardingRule;
import lombok.Getter;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Sharding schema.
 *
 * @author zhangliang
 * @author zhangyonglun
 * @author panjuan
 * @author zhaojun
 * @author wangkai
 */
@Getter
public final class ShardingSchema extends LogicSchema {
    
    private ShardingRule shardingRule;
    
    public ShardingSchema(final String name, final Map<String, DataSourceParameter> dataSources, final ShardingRuleConfiguration shardingRuleConfig, final boolean isUsingRegistry) {
        super(name, dataSources, getShardingRule(shardingRuleConfig, dataSources.keySet(), isUsingRegistry));
        shardingRule = getShardingRule(shardingRuleConfig, dataSources.keySet(), isUsingRegistry);
    }
    
    private static ShardingRule getShardingRule(final ShardingRuleConfiguration shardingRule, final Collection<String> dataSourceNames, final boolean isUsingRegistry) {
        return isUsingRegistry ? new OrchestrationShardingRule(shardingRule, dataSourceNames) : new ShardingRule(shardingRule, dataSourceNames);
    }
    
    /**
     * Renew sharding rule.
     *
     * @param shardingEvent sharding event.
     */
    @Subscribe
    public void renew(final ShardingRuleChangedEvent shardingEvent) {
        if (!getName().equals(shardingEvent.getShardingSchemaName())) {
            return;
        }
        shardingRule = new OrchestrationShardingRule(shardingEvent.getShardingRuleConfiguration(), getDataSources().keySet());
    }
    
    /**
     * Renew disabled data source names.
     *
     * @param disabledStateEventBusEvent jdbc disabled event bus event
     */
    @Subscribe
    public void renew(final DisabledStateEventBusEvent disabledStateEventBusEvent) {
        Map<String, Collection<String>> disabledSchemaDataSourceMap = disabledStateEventBusEvent.getDisabledSchemaDataSourceMap();
        if (!disabledSchemaDataSourceMap.keySet().contains(getName())) {
            return;
        }
        renew(disabledSchemaDataSourceMap.get(getName()));
    }
    
    private void renew(final Collection<String> disabledDataSourceNames) {
        for (MasterSlaveRule each : shardingRule.getMasterSlaveRules()) {
            DisabledStateEventBusEvent eventBusEvent = new DisabledStateEventBusEvent(Collections.singletonMap(ShardingConstant.LOGIC_SCHEMA_NAME, disabledDataSourceNames));
            ((OrchestrationMasterSlaveRule) each).renew(eventBusEvent);
        }
    }
}
