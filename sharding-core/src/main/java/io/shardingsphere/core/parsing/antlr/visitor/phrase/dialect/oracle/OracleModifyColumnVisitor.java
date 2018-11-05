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

package io.shardingsphere.core.parsing.antlr.visitor.phrase.dialect.oracle;

import com.google.common.base.Optional;
import io.shardingsphere.core.parsing.antlr.RuleName;
import io.shardingsphere.core.parsing.antlr.sql.ddl.AlterTableStatement;
import io.shardingsphere.core.parsing.antlr.sql.ddl.ColumnDefinition;
import io.shardingsphere.core.parsing.antlr.util.ASTUtils;
import io.shardingsphere.core.parsing.antlr.util.VisitorUtils;
import io.shardingsphere.core.parsing.antlr.visitor.phrase.PhraseVisitor;
import io.shardingsphere.core.parsing.parser.sql.SQLStatement;
import org.antlr.v4.runtime.ParserRuleContext;

/**
 * Visit oracle modify column phrase.
 * 
 * @author duhongjun
 */
public final class OracleModifyColumnVisitor implements PhraseVisitor {
    
    @Override
    public void visit(final ParserRuleContext ancestorNode, final SQLStatement statement) {
        AlterTableStatement alterStatement = (AlterTableStatement) statement;
        for (ParserRuleContext modifyColumnContext : ASTUtils.getAllDescendantNodes(ancestorNode, RuleName.MODIFY_COLUMN)) {
            for (ParserRuleContext each : ASTUtils.getAllDescendantNodes(modifyColumnContext, RuleName.MODIFY_COL_PROPERTIES)) {
                // it`s not column definition, but can call this method
                Optional<ColumnDefinition> column = VisitorUtils.visitColumnDefinition(each);
                if (column.isPresent()) {
                    alterStatement.getUpdateColumns().put(column.get().getName(), column.get());
                }
            }
        }
    }
}
