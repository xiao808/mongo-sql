package com.github.xiao808.mongo.sql.execution;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;

/**
 * delete action for mongo
 * only simple delete sql clause can be parsed
 * eg: delete from table where a = xxx and b = xxx or c = xxx and d between xxx and xxx
 *
 * @author zengxiao
 * @date 2023/3/22 16:25
 * @since 1.0
 **/
public class SqlDeleteExecution extends AbstractSqlExecution {

    public SqlDeleteExecution(SQLDeleteStatement sqlDeleteStatement) {
        super(sqlDeleteStatement);
    }

    @Override
    public JsonNode execute(MongoDatabase mongoDatabase) {
        SQLDeleteStatement deleteStatement = (SQLDeleteStatement) this.sqlStatement;
        sqlStatementTransformVisitor.visit(deleteStatement);
        String tableName = deleteStatement.getTableName().getSimpleName();
        SQLExpr where = deleteStatement.getWhere();
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(tableName);
        DeleteResult deleteResult = mongoCollection.deleteMany(sqlStatementTransformVisitor.getWhere(where));
        return new LongNode(deleteResult.wasAcknowledged() ? deleteResult.getDeletedCount() : 0L);
    }
}
