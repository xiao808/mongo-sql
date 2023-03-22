package com.github.xiao808.mongo.sql.execution;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.xiao808.mongo.sql.InheritableThreadLocalMongoContextHolder;
import com.github.xiao808.mongo.sql.MongoContext;
import com.github.xiao808.mongo.sql.visitor.SqlTransformToMongoVisitor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;

/**
 * @author zengxiao
 * @description
 * @date 2023/3/22 16:25
 * @since 1.0
 **/
public class SqlDeleteExecution implements SqlExecution {

    @Override
    public void accept(SqlTransformToMongoVisitor sqlTransformToMongoVisitor) {
        MongoContext mongoContext = InheritableThreadLocalMongoContextHolder.getContext();
        SQLStatement sqlStatement = mongoContext.getSqlStatement();
        sqlTransformToMongoVisitor.visit((SQLDeleteStatement) sqlStatement);
    }

    @Override
    public JsonNode execute(MongoDatabase mongoDatabase) {
        MongoContext mongoContext = InheritableThreadLocalMongoContextHolder.getContext();
        DeleteResult deleteResult = mongoDatabase.getCollection("").deleteMany(mongoContext.getCondition());
        return null;
    }
}
