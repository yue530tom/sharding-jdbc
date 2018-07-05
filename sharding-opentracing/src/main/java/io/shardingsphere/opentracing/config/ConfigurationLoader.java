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

package io.shardingsphere.opentracing.config;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import lombok.Getter;

/**
 * Config loader.
 *
 * @author gaohongtao
 * @author wangkai
 */
public final class ConfigurationLoader {
    
    private static final ConfigurationParser[] PARSERS = new ConfigurationParser[]{new OpentracingConfigurationParser()};
    
    @Getter
    private final String tracerClassName;
    
    @Getter
    private final int sampleNumPM;
    
    public ConfigurationLoader() {
        String tracerClassName = null;
        int maxSampleNum = 0;
        for (ConfigurationParser each : PARSERS) {
            Optional<String> sampleNumPMOptional = each.parse("tracer.sampleNumPM");
            if (sampleNumPMOptional.isPresent()) {
                maxSampleNum = Integer.parseInt(sampleNumPMOptional.get());
            }
            Optional<String> tracerClassOptional = each.parse("tracer.class");
            if (tracerClassOptional.isPresent()) {
                tracerClassName = tracerClassOptional.get();
                break;
            }
        }
        Preconditions.checkNotNull(tracerClassName);
        this.tracerClassName = tracerClassName;
        this.sampleNumPM = maxSampleNum;
    }
}
