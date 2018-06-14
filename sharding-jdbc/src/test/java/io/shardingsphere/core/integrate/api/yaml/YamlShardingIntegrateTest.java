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

package io.shardingsphere.core.integrate.api.yaml;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.shardingsphere.core.api.ConfigMapContext;
import io.shardingsphere.core.api.yaml.YamlShardingDataSourceFactory;
import io.shardingsphere.core.constant.ShardingProperties;
import io.shardingsphere.core.constant.ShardingPropertiesConstant;
import lombok.RequiredArgsConstructor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
@RequiredArgsConstructor
public class YamlShardingIntegrateTest extends AbstractYamlDataSourceTest {
    
    private final String filePath;
    
    private final boolean hasDataSource;
    
    @Parameters(name = "{index}:{0}-{1}")
    public static Collection init() {
        return Arrays.asList(new Object[][]{
                {"/integrate/api/yaml/sharding/configWithDataSourceWithoutProps.yaml", true},
                {"/integrate/api/yaml/sharding/configWithoutDataSourceWithoutProps.yaml", false},
                {"/integrate/api/yaml/sharding/configWithDataSourceWithProps.yaml", true},
                {"/integrate/api/yaml/sharding/configWithoutDataSourceWithProps.yaml", false},
        });
    }
    
    @Test
    public void assertWithDataSource() throws SQLException, URISyntaxException, IOException, ReflectiveOperationException {
        File yamlFile = new File(YamlShardingIntegrateTest.class.getResource(filePath).toURI());
        DataSource dataSource;
        if (hasDataSource) {
            dataSource = YamlShardingDataSourceFactory.createDataSource(yamlFile);
        } else {
            dataSource = YamlShardingDataSourceFactory.createDataSource(Maps.asMap(Sets.newHashSet("db0", "db1"), new Function<String, DataSource>() {
                @Override
                public DataSource apply(final String key) {
                    return createDataSource(key);
                }
            }), yamlFile);
        }
        if (filePath.contains("WithProps.yaml")) {
            Field field = dataSource.getClass().getDeclaredField("shardingProperties");
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            ShardingProperties shardingProperties = (ShardingProperties) field.get(dataSource);
            assertTrue((Boolean) shardingProperties.getValue(ShardingPropertiesConstant.SQL_SHOW));
        }
        Map<String, Object> configMap = new ConcurrentHashMap<>();
        configMap.put("key1", "value1");
        assertThat(ConfigMapContext.getInstance().getShardingConfig(), is(configMap));
        try (Connection conn = dataSource.getConnection();
             Statement stm = conn.createStatement()) {
            stm.execute(String.format("INSERT INTO t_order(user_id,status) values(%d, %s)", 10, "'insert'"));
            stm.executeQuery("SELECT o.*, i.* FROM T_order o JOIN T_order_item i ON o.order_id = i.order_id");
            stm.executeQuery("SELECT * FROM config");
        }
    }
}
