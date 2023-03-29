package com.github.xiao808.mongo.sql.execution;

import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
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
        System.err.println("delete.......................");
        return null;
    }
}
