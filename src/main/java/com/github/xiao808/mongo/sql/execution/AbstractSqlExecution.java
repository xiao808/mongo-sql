package com.github.xiao808.mongo.sql.execution;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.xiao808.mongo.sql.visitor.SqlStatementTransformVisitor;
import com.mongodb.client.MongoDatabase;

import java.util.Objects;

/**
 * sql execution interface
 *
 * @author zengxiao
 * @date 2023/3/22 15:45
 * @since 1.0
 **/
public abstract class AbstractSqlExecution {

    protected SQLStatement sqlStatement;

    protected SqlStatementTransformVisitor sqlStatementTransformVisitor = new SqlStatementTransformVisitor();

    public AbstractSqlExecution(SQLStatement sqlStatement) {
        this.sqlStatement = sqlStatement;
    }

    public static <T> AbstractSqlExecution getInstance(SQLStatement sqlStatement) {
        return AbstractSqlExecution.getInstance(sqlStatement, true, 2000);
    }

    /**
     * sql execution factory
     *
     * @return sql execution
     */
    public static <T> AbstractSqlExecution getInstance(SQLStatement sqlStatement, boolean aggregationAllowDiskUse, int aggregationBatchSize) {
        // can not be parsed whiling SQLStatement is null.
        Objects.requireNonNull(sqlStatement, "empty mongo context for execution.");
        // according to SQLStatement type gain the special sql execution.
        if (sqlStatement instanceof SQLSelectStatement) {
            // for select
            return new SqlSelectExecution((SQLSelectStatement) sqlStatement, aggregationAllowDiskUse, aggregationBatchSize);
        }
        if (sqlStatement instanceof SQLInsertStatement) {
            // for insert
            return new SqlInsertExecution((SQLInsertStatement) sqlStatement);
        }
        if (sqlStatement instanceof SQLUpdateStatement) {
            // for update
            return new SqlUpdateExecution((SQLUpdateStatement) sqlStatement);
        }
        if (sqlStatement instanceof SQLDeleteStatement) {
            // for delete
            return new SqlDeleteExecution((SQLDeleteStatement) sqlStatement);
        }
        throw new IllegalArgumentException("illegal mongo context for execution.");
    }

    /**
     * transform sql and execute
     *
     * @param mongoDatabase mongo database
     * @return execute result
     */
    public abstract JsonNode execute(MongoDatabase mongoDatabase);

    /**
     * get sql statement transform visitor
     *
     * @return sql statement transform visitor
     */
    public SqlStatementTransformVisitor getSqlStatementTransformVisitor() {
        return sqlStatementTransformVisitor;
    }
}
