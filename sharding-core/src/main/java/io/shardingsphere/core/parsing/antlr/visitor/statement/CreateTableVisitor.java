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

package io.shardingsphere.core.parsing.antlr.visitor.statement;

import io.shardingsphere.core.metadata.table.ColumnMetaData;
import io.shardingsphere.core.metadata.table.TableMetaData;
import io.shardingsphere.core.parsing.antlr.visitor.phrase.ColumnDefinitionVisitor;
import io.shardingsphere.core.parsing.antlr.visitor.phrase.CreatePrimaryKeyVisitor;
import io.shardingsphere.core.parsing.antlr.visitor.phrase.IndexesNameVisitor;
import io.shardingsphere.core.parsing.antlr.visitor.phrase.TableNamesVisitor;
import io.shardingsphere.core.parsing.parser.sql.SQLStatement;
import io.shardingsphere.core.parsing.parser.sql.ddl.create.table.CreateTableStatement;

import java.util.LinkedList;
import java.util.List;

/**
 * create table statement visitor.
 * 
 * @author duhongjun
 */
public final class CreateTableVisitor extends DDLStatementVisitor {
    
    public CreateTableVisitor() {
        addVisitor(new TableNamesVisitor());
        addVisitor(new ColumnDefinitionVisitor());
        addVisitor(new IndexesNameVisitor());
        addVisitor(new CreatePrimaryKeyVisitor());
    }
    
    @Override
    protected SQLStatement newStatement() {
        return new CreateTableStatement();
    }
    
    @Override
    protected void postVisit(final SQLStatement statement) {
        CreateTableStatement createStatement = (CreateTableStatement) statement;
        List<ColumnMetaData> newColumnMeta = new LinkedList<>();
        int position = 0;
        List<String> columnTypes = createStatement.getColumnTypes();
        List<String> primaryKeyColumns = createStatement.getPrimaryKeyColumns();
        for (String each : createStatement.getColumnNames()) {
            String type = null;
            if (columnTypes.size() > position) {
                type = columnTypes.get(position);
            }
            newColumnMeta.add(new ColumnMetaData(each, type, primaryKeyColumns.contains(each)));
        }
        createStatement.setTableMetaData(new TableMetaData(newColumnMeta));
    }
}
