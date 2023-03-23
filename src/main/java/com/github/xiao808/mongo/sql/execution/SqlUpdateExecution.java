package com.github.xiao808.mongo.sql.execution;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLListExpr;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.ast.statement.SQLWithSubqueryClause;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.xiao808.mongo.sql.InheritableThreadLocalMongoContextHolder;
import com.github.xiao808.mongo.sql.MongoContext;
import com.mongodb.client.MongoDatabase;

import java.util.List;

/**
 * @author zengxiao
 * @description
 * @date 2023/3/22 16:24
 * @since 1.0
 **/
public class SqlUpdateExecution implements SqlExecution {

    @Override
    public JsonNode execute(MongoDatabase mongoDatabase) {
        MongoContext mongoContext = InheritableThreadLocalMongoContextHolder.getContext();
        SQLUpdateStatement sqlStatement = (SQLUpdateStatement) mongoContext.getSqlStatement();
        SQLWithSubqueryClause with = sqlStatement.getWith();
        if (with != null) {

        }

        SQLTableSource table = sqlStatement.getTableSource();
        SQLTableSource from = sqlStatement.getFrom();

        if (from != null) {
        }

        List<SQLUpdateSetItem> items = sqlStatement.getItems();
        for (SQLUpdateSetItem item : items) {
            SQLExpr column = item.getColumn();
            if (column instanceof SQLIdentifierExpr) {
                SQLIdentifierExpr identifierExpr = (SQLIdentifierExpr) column;
                identifierExpr.setResolvedTableSource(table);
            } else if (column instanceof SQLListExpr) {
                SQLListExpr columnGroup = (SQLListExpr) column;
                for (SQLExpr columnGroupItem : columnGroup.getItems()) {
                    if (columnGroupItem instanceof SQLIdentifierExpr) {
                        SQLIdentifierExpr identifierExpr = (SQLIdentifierExpr) columnGroupItem;
                        identifierExpr.setResolvedTableSource(table);
                    } else {
                    }
                }
            } else {
            }
            SQLExpr value = item.getValue();
            if (value != null) {
            }
        }

        SQLExpr where = sqlStatement.getWhere();
        if (where != null) {
        }

        SQLOrderBy orderBy = sqlStatement.getOrderBy();
        if (orderBy != null) {
        }

        for (SQLExpr sqlExpr : sqlStatement.getReturning()) {
        }
        return null;
    }
}
