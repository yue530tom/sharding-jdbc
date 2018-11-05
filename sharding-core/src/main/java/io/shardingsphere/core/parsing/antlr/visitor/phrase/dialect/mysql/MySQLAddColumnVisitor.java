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

package io.shardingsphere.core.parsing.antlr.visitor.phrase.dialect.mysql;

import com.google.common.base.Optional;
import io.shardingsphere.core.parsing.antlr.sql.ddl.ColumnPosition;
import io.shardingsphere.core.parsing.antlr.sql.ddl.mysql.MySQLAlterTableStatement;
import io.shardingsphere.core.parsing.antlr.util.VisitorUtils;
import io.shardingsphere.core.parsing.antlr.visitor.phrase.AddColumnVisitor;
import io.shardingsphere.core.parsing.parser.sql.SQLStatement;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * Visit MySQL add column phrase.
 * 
 * @author duhongjun
 */
public final class MySQLAddColumnVisitor extends AddColumnVisitor {
    
    @Override
    protected void postVisitColumnDefinition(final ParseTree ancestorNode, final SQLStatement statement, final String columnName) {
        Optional<ColumnPosition> columnPosition = VisitorUtils.visitFirstOrAfterColumn((ParserRuleContext) ancestorNode, columnName);
        MySQLAlterTableStatement alterStatement = (MySQLAlterTableStatement) statement;
        if (columnPosition.isPresent()) {
            alterStatement.getPositionChangedColumns().add(columnPosition.get());
        }
    }
}
