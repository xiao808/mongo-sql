package com.github.xiao808.mongo.sql.execution;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.ast.statement.SQLWithSubqueryClause;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.xiao808.mongo.sql.InheritableThreadLocalMongoContextHolder;
import com.github.xiao808.mongo.sql.MongoContext;
import com.mongodb.client.MongoDatabase;

/**
 * @author zengxiao
 * @description
 * @date 2023/3/22 16:24
 * @since 1.0
 **/
public class SqlInsertExecution implements SqlExecution {

    @Override
    public JsonNode execute(MongoDatabase mongoDatabase) {
        MongoContext mongoContext = InheritableThreadLocalMongoContextHolder.getContext();
        SQLInsertStatement sqlStatement = (SQLInsertStatement) mongoContext.getSqlStatement();
        SQLWithSubqueryClause with = sqlStatement.getWith();
        if (with != null) {
        }

        SQLTableSource table = sqlStatement.getTableSource();

        if (table != null) {
        }

        for (SQLExpr column : sqlStatement.getColumns()) {
        }

        for (SQLInsertStatement.ValuesClause valuesClause : sqlStatement.getValuesList()) {
        }

        SQLSelect query = sqlStatement.getQuery();
        if (query != null) {
        }
        return null;
    }
}
