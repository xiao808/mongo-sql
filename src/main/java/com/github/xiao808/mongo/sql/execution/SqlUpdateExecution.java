package com.github.xiao808.mongo.sql.execution;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLNullExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.github.xiao808.mongo.sql.utils.SqlUtils;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.github.xiao808.mongo.sql.LexerConstants.SET;
import static com.github.xiao808.mongo.sql.LexerConstants.UNSET;

/**
 * update action for mongo
 * only simple sql clause can be parsed
 * eg: update table set a = xxx, b = xxx where c = xxx and d = xxx or e is null
 *
 * @author zengxiao
 * @date 2023/3/22 16:24
 * @since 1.0
 **/
public class SqlUpdateExecution extends AbstractSqlExecution {

    public SqlUpdateExecution(SQLUpdateStatement sqlUpdateStatement) {
        super(sqlUpdateStatement);
    }

    @Override
    public JsonNode execute(MongoDatabase mongoDatabase) {
        SQLUpdateStatement updateStatement = (SQLUpdateStatement) this.sqlStatement;
        sqlStatementTransformVisitor.visit(updateStatement);
        SQLExprTableSource tableSource = (SQLExprTableSource) updateStatement.getTableSource();
        String tableName = tableSource.getTableName();
        SQLExpr whereClause = updateStatement.getWhere();
        List<SQLUpdateSetItem> items = updateStatement.getItems();
        Document updateSet = new Document();
        for (SQLUpdateSetItem item : items.stream().filter(sqlUpdateSetItem -> !(sqlUpdateSetItem.getValue() instanceof SQLNullExpr)).collect(Collectors.toList())) {
            updateSet.put(SqlUtils.getColumnNameWithOutTableAlias(item.getColumn()), SqlUtils.getRealValue(item.getValue()));
        }
        List<String> fieldsToUnset = new ArrayList<>();
        for (SQLUpdateSetItem item : items.stream().filter(sqlUpdateSetItem -> sqlUpdateSetItem.getValue() instanceof SQLNullExpr).collect(Collectors.toList())) {
            fieldsToUnset.add(SqlUtils.getColumnNameWithOutTableAlias(item.getColumn()));
        }
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(tableName);
        UpdateResult updateResult = null;
        Document where = sqlStatementTransformVisitor.getWhere(whereClause);
        if (!updateSet.isEmpty() && !fieldsToUnset.isEmpty()) {
            updateResult = mongoCollection.updateMany(where, List.of(new Document(SET, updateSet), new Document(UNSET, fieldsToUnset)));
        } else if (!updateSet.isEmpty()) {
            updateResult = mongoCollection.updateMany(where, new Document(SET, updateSet));
        } else if (!fieldsToUnset.isEmpty()) {
            updateResult = mongoCollection.updateMany(where, new Document(UNSET, fieldsToUnset));
        }
        UpdateResult result = Optional.ofNullable(updateResult).orElseGet(() -> UpdateResult.acknowledged(0, 0L, null));
        return new LongNode(result.wasAcknowledged() ? result.getModifiedCount() : 0L);
    }
}
