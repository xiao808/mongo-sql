package com.github.xiao808.mongo.sql.execution;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.xiao808.mongo.sql.InheritableThreadLocalMongoContextHolder;
import com.github.xiao808.mongo.sql.MongoContext;
import com.github.xiao808.mongo.sql.visitor.SqlTransformToMongoVisitor;
import com.mongodb.client.MongoDatabase;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author zengxiao
 * @description sql执行接口
 * @date 2023/3/22 15:45
 * @since 1.0
 **/
public interface SqlExecution extends Consumer<SqlTransformToMongoVisitor> {

    /**
     * sql execution factory
     *
     * @return sql execution
     */
    static <T> SqlExecution getInstance() {
        // get current mongo context
        MongoContext mongoContext = InheritableThreadLocalMongoContextHolder.getContext();
        SQLStatement sqlStatement = mongoContext.getSqlStatement();
        // can not be parsed whiling SQLStatement is null.
        Objects.requireNonNull(sqlStatement, "empty mongo context for execution.");
        // according to SQLStatement type gain the special sql execution.
        if (sqlStatement instanceof SQLSelectStatement) {
            // for select
            return new SqlSelectExecution();
        }
        if (sqlStatement instanceof SQLInsertStatement) {
            // for insert
            return new SqlInsertExecution();
        }
        if (sqlStatement instanceof SQLUpdateStatement) {
            // for update
            return new SqlUpdateExecution();
        }
        if (sqlStatement instanceof SQLDeleteStatement) {
            // for delete
            return new SqlDeleteExecution();
        }
        throw new IllegalArgumentException("illegal mongo context for execution.");
    }

    JsonNode execute(MongoDatabase mongoDatabase);
}
