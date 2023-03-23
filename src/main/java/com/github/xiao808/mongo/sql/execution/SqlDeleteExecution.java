package com.github.xiao808.mongo.sql.execution;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.ast.statement.SQLWithSubqueryClause;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.xiao808.mongo.sql.InheritableThreadLocalMongoContextHolder;
import com.github.xiao808.mongo.sql.MongoContext;
import com.mongodb.client.MongoDatabase;

/**
 * @author zengxiao
 * @description
 * @date 2023/3/22 16:25
 * @since 1.0
 **/
public class SqlDeleteExecution implements SqlExecution {

    @Override
    public JsonNode execute(MongoDatabase mongoDatabase) {
        MongoContext mongoContext = InheritableThreadLocalMongoContextHolder.getContext();
        SQLDeleteStatement sqlStatement = (SQLDeleteStatement) mongoContext.getSqlStatement();
        String tableName = null;
        SQLWithSubqueryClause with = sqlStatement.getWith();
        if (with != null) {
        }

        SQLTableSource table = sqlStatement.getTableSource();
        SQLTableSource from = sqlStatement.getFrom();

        if (from == null) {
            from = sqlStatement.getUsing();
        }

        if (table == null && from != null) {
            table = from;
            from = null;
        }

        if (from != null) {
        }

        if (table != null) {
            if (from != null && table instanceof SQLExprTableSource) {
                SQLExpr tableExpr = ((SQLExprTableSource) table).getExpr();
                if (tableExpr instanceof SQLPropertyExpr
                        && ((SQLPropertyExpr) tableExpr).getName().equals("*")) {
                    String alias = ((SQLPropertyExpr) tableExpr).getOwnernName();
                    SQLTableSource refTableSource = from.findTableSource(alias);
                    if (refTableSource != null) {
                        ((SQLPropertyExpr) tableExpr).setResolvedTableSource(refTableSource);
                    }
                }
            }
        }

        SQLExpr where = sqlStatement.getWhere();
        if (where != null) {
        }
        return null;
    }
}
