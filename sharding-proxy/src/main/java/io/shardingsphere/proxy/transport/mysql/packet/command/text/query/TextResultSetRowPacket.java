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

package io.shardingsphere.proxy.transport.mysql.packet.command.text.query;

import io.shardingsphere.proxy.transport.mysql.packet.MySQLPacket;
import io.shardingsphere.proxy.transport.mysql.packet.MySQLPacketPayload;
import lombok.Getter;

import java.util.List;

import com.google.common.collect.Lists;

/**
 * Text result set row packet.
 * @see <a href="https://dev.mysql.com/doc/internals/en/com-query-response.html#packet-ProtocolText::ResultsetRow">ResultsetRow</a>
 *
 * @author zhangliang
 */
@Getter
public final class TextResultSetRowPacket extends MySQLPacket {
    
    private static final int NULL = 0xfb;
    
    private final List<Object> data;
    
    public TextResultSetRowPacket(final int sequenceId, final List<Object> data) {
        super(sequenceId);
        this.data = data;
    }
    
    public TextResultSetRowPacket(final MySQLPacketPayload mysqlPacketPayload, final int columnCount) {
        super(mysqlPacketPayload.readInt1());
        data = Lists.newArrayListWithExpectedSize(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            data.add(mysqlPacketPayload.readStringLenenc());
        }
    }
    
    @Override
    public void write(final MySQLPacketPayload mysqlPacketPayload) {
        for (Object each : data) {
            if (null == each) {
                mysqlPacketPayload.writeInt1(NULL);
            } else {
                mysqlPacketPayload.writeStringLenenc(each.toString());
            }
        }
    }
}
