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
import io.shardingsphere.core.metadata.table.ShardingTableMetaData;
import io.shardingsphere.core.metadata.table.TableMetaData;
import io.shardingsphere.core.parsing.antlr.sql.ddl.AlterTableStatement;
import io.shardingsphere.core.parsing.antlr.sql.ddl.ColumnDefinition;
import io.shardingsphere.core.parsing.antlr.visitor.phrase.DropColumnVisitor;
import io.shardingsphere.core.parsing.antlr.visitor.phrase.RenameTableVisitor;
import io.shardingsphere.core.parsing.antlr.visitor.phrase.TableNamesVisitor;
import io.shardingsphere.core.parsing.parser.sql.SQLStatement;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Abstract statement visitor, get information by each phrase visitor.
 * 
 * @author duhongjun
 */
public abstract class AlterTableVisitor extends DDLStatementVisitor {
    
    public AlterTableVisitor() {
        addVisitor(new TableNamesVisitor());
        addVisitor(new RenameTableVisitor());
        addVisitor(new DropColumnVisitor());
    }
    
    protected void postVisit(final SQLStatement statement) {
        AlterTableStatement alterStatement = (AlterTableStatement) statement;
        TableMetaData oldTableMeta = alterStatement.getTableMetaDataMap().get(alterStatement.getTables().getSingleTableName());
        if (null == oldTableMeta) {
            return;
        }
        List<ColumnMetaData> newColumnMeta = updateColumn(alterStatement, oldTableMeta);
        addColumn(alterStatement, newColumnMeta);
        adjustColumn(alterStatement, newColumnMeta);
        dropColumn(alterStatement, newColumnMeta);
        alterStatement.setTableMetaData(new TableMetaData(newColumnMeta));
    }
    
    protected void adjustColumn(final AlterTableStatement alterStatement, final List<ColumnMetaData> newColumnMeta) {
    }
    
    private List<ColumnMetaData> updateColumn(final AlterTableStatement alterStatement, final TableMetaData oldTableMeta) {
        List<ColumnMetaData> result = new LinkedList<>();
        for (ColumnMetaData each : oldTableMeta.getColumnMetaData()) {
            ColumnDefinition columnDefinition = alterStatement.getUpdateColumns().get(each.getColumnName());
            String columnName;
            String columnType;
            boolean primaryKey = false;
            if (null == columnDefinition) {
                columnName = each.getColumnName();
                columnType = each.getColumnType();
                primaryKey = each.isPrimaryKey();
            } else {
                columnName = columnDefinition.getName();
                columnType = columnDefinition.getType();
                if (columnDefinition.isPrimaryKey()) {
                    primaryKey = columnDefinition.isPrimaryKey();
                }
            }
            if (each.isPrimaryKey() && alterStatement.isDropPrimaryKey()) {
                primaryKey = false;
            }
            result.add(new ColumnMetaData(columnName, columnType, primaryKey));
        }
        return result;
    }
    
    private void addColumn(final AlterTableStatement alterStatement, final List<ColumnMetaData> newColumnMeta) {
        for (ColumnDefinition each : alterStatement.getAddColumns()) {
            newColumnMeta.add(new ColumnMetaData(each.getName(), each.getType(), each.isPrimaryKey()));
        }
    }
    
    private void dropColumn(final AlterTableStatement alterStatement, final List<ColumnMetaData> newColumnMeta) {
        Iterator<ColumnMetaData> it = newColumnMeta.iterator();
        while (it.hasNext()) {
            ColumnMetaData each = it.next();
            if (alterStatement.getDropColumns().contains(each.getColumnName())) {
                it.remove();
            }
        }
    }
    
    protected final SQLStatement newStatement(final ShardingTableMetaData shardingTableMetaData) {
        AlterTableStatement result = (AlterTableStatement) newStatement();
        result.setTableMetaDataMap(shardingTableMetaData);
        return result;
    }
    
    protected SQLStatement newStatement() {
        return new AlterTableStatement();
    }
}
