package com.github.xiao808.mongo.sql.execution;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.xiao808.mongo.sql.InheritableThreadLocalMongoContextHolder;
import com.github.xiao808.mongo.sql.MongoContext;
import com.github.xiao808.mongo.sql.visitor.SqlTransformToMongoVisitor;
import com.mongodb.client.MongoDatabase;

/**
 * @author zengxiao
 * @description
 * @date 2023/3/22 16:24
 * @since 1.0
 **/
public class SqlUpdateExecution implements SqlExecution {

    @Override
    public void accept(SqlTransformToMongoVisitor sqlTransformToMongoVisitor) {
        MongoContext mongoContext = InheritableThreadLocalMongoContextHolder.getContext();
        SQLStatement sqlStatement = mongoContext.getSqlStatement();
        sqlTransformToMongoVisitor.visit((SQLUpdateStatement) sqlStatement);
    }

    @Override
    public JsonNode execute(MongoDatabase mongoDatabase) {
        return null;
    }
}
