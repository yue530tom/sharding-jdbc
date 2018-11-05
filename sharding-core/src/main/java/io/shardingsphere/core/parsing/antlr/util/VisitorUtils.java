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

package io.shardingsphere.core.parsing.antlr.util;

import com.google.common.base.Optional;
import io.shardingsphere.core.parsing.antlr.RuleName;
import io.shardingsphere.core.parsing.antlr.sql.ddl.ColumnDefinition;
import io.shardingsphere.core.parsing.antlr.sql.ddl.ColumnPosition;
import io.shardingsphere.core.parsing.lexer.token.Symbol;
import io.shardingsphere.core.parsing.parser.token.IndexToken;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.antlr.v4.runtime.ParserRuleContext;

/**
 * Visitor utility.
 * 
 * @author duhongjun
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VisitorUtils {
    
    /**
     * Parse column definition.
     *
     * @param columnDefinitionNode column definition rule
     * @return column definition
     */
    public static Optional<ColumnDefinition> visitColumnDefinition(final ParserRuleContext columnDefinitionNode) {
        Optional<ParserRuleContext> columnNameNode = ASTUtils.findFirstChildNode(columnDefinitionNode, RuleName.COLUMN_NAME);
        if (!columnNameNode.isPresent()) {
            return Optional.absent();
        }
        Optional<ParserRuleContext> dataTypeContext = ASTUtils.findFirstChildNode(columnDefinitionNode, RuleName.DATA_TYPE);
        Optional<String> typeName = dataTypeContext.isPresent() ? Optional.of(dataTypeContext.get().getChild(0).getText()) : Optional.<String>absent();
        Optional<Integer> dataTypeLength = dataTypeContext.isPresent() ? getDataTypeLength(dataTypeContext.get()) : Optional.<Integer>absent();
        boolean primaryKey = ASTUtils.findFirstChildNode(columnDefinitionNode, RuleName.PRIMARY_KEY).isPresent();
        return Optional.of(new ColumnDefinition(columnNameNode.get().getText(), typeName.orNull(), dataTypeLength.orNull(), primaryKey));
    }
    
    private static Optional<Integer> getDataTypeLength(final ParserRuleContext dataTypeContext) {
        Optional<ParserRuleContext> dataTypeLengthContext = ASTUtils.findFirstChildNode(dataTypeContext, RuleName.DATA_TYPE_LENGTH);
        if (!dataTypeLengthContext.isPresent() || dataTypeLengthContext.get().getChildCount() < 3) {
            return Optional.absent();
        }
        try {
            return Optional.of(Integer.parseInt(dataTypeLengthContext.get().getChild(1).getText()));
        } catch (final NumberFormatException ignored) {
            return Optional.absent();
        }
    }
    
    /**
     * Visit column position.
     *
     * @param ancestorNode ancestor node of AST
     * @param columnName column name
     * @return column position object
     */
    public static Optional<ColumnPosition> visitFirstOrAfterColumn(final ParserRuleContext ancestorNode, final String columnName) {
        Optional<ParserRuleContext> firstOrAfterColumnContext = ASTUtils.findFirstChildNode(ancestorNode, RuleName.FIRST_OR_AFTER_COLUMN);
        if (!firstOrAfterColumnContext.isPresent()) {
            return Optional.absent();
        }
        Optional<ParserRuleContext> columnNameContext = ASTUtils.findFirstChildNode(firstOrAfterColumnContext.get(), RuleName.COLUMN_NAME);
        ColumnPosition result = new ColumnPosition();
        result.setStartIndex(firstOrAfterColumnContext.get().getStart().getStartIndex());
        if (columnNameContext.isPresent()) {
            result.setColumnName(columnName);
            result.setAfterColumn(columnNameContext.get().getText());
        } else {
            result.setFirstColumn(columnName);
        }
        return Optional.of(result);
    }
    
    /**
     * Visit index node.
     *
     * @param indexNameContext index name context
     * @param tableName  table name
     * @return index token
     */
    public static IndexToken visitIndex(final ParserRuleContext indexNameContext, final String tableName) {
        return new IndexToken(indexNameContext.getStop().getStartIndex(), getIndexName(indexNameContext.getText()), tableName);
    }
    
    private static String getIndexName(final String text) {
        return text.contains(Symbol.DOT.getLiterals()) ? text.substring(text.lastIndexOf(Symbol.DOT.getLiterals()) + Symbol.DOT.getLiterals().length()) : text;
    }
}
