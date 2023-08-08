package com.github.xiao808.mongo.sql.visitor;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLNullExpr;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.visitor.SQLASTVisitor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.github.xiao808.mongo.sql.utils.SqlUtils;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.github.xiao808.mongo.sql.LexerConstants.SET;
import static com.github.xiao808.mongo.sql.LexerConstants.UNSET;

/**
 * @author zengxiao
 * @date 2023/8/4 17:24
 * @since 1.0
 **/
public interface SqlVisitor extends SQLASTVisitor {


    /**
     * is aggregate query
     *
     * @return whether sql is aggregate query
     */
    boolean isAggregate();

    /**
     * is distinct query
     *
     * @return whether sql is distinct query
     */
    boolean isDistinct();

    /**
     * get distinct field
     *
     * @return distinct field
     */
    String getDistinctField();

    /**
     * is count(*) query
     *
     * @return whether sql is count(*) query
     */
    boolean isCountAll();

    /**
     * get result parsed of any sql object
     *
     * @param sqlObject any node of sql AST, wrong argument will return empty document
     * @return result parsed of node
     */
    Document getDocument(SQLObject sqlObject);

    /**
     * get result parsed of where clause
     *
     * @param where where clause, wrong argument will return empty document
     * @return result parsed of where clause
     */
    Document getWhere(SQLExpr where);

    /**
     * get aggregate document list of sql select statement
     *
     * @param sqlSelectStatement sql select statement, wrong argument will return empty document
     * @return aggregate document list of sql select
     */
    List<Document> getAggregation(SQLSelectStatement sqlSelectStatement);

    /**
     * get aggregate table name of sql select statement
     *
     * @param sqlSelectStatement sql select statement
     * @return base table name of aggregation
     */
    String getAggregationTableName(SQLSelectStatement sqlSelectStatement);

    /**
     * transform sql and execute
     *
     * @param sqlStatement            sql statement
     * @param mongoDatabase           mongo database
     * @param aggregationAllowDiskUse whether disk using is allowed whiling aggregating
     * @param aggregationBatchSize    batch size of aggregation
     * @return execute result
     */
    default JsonNode execute(SQLStatement sqlStatement, MongoDatabase mongoDatabase, boolean aggregationAllowDiskUse, int aggregationBatchSize) {
        // can not be parsed whiling SQLStatement is null.
        Objects.requireNonNull(sqlStatement, "empty mongo context for execution.");
        // according to SQLStatement type gain the special sql execution.
        if (sqlStatement instanceof SQLSelectStatement) {
            // for select
            return this.executeSelect((SQLSelectStatement) sqlStatement, mongoDatabase, aggregationAllowDiskUse, aggregationBatchSize);
        }
        if (sqlStatement instanceof SQLInsertStatement) {
            // for insert
            return this.executeInsert((SQLInsertStatement) sqlStatement, mongoDatabase);
        }
        if (sqlStatement instanceof SQLUpdateStatement) {
            // for update
            return this.executeUpdate((SQLUpdateStatement) sqlStatement, mongoDatabase);
        }
        if (sqlStatement instanceof SQLDeleteStatement) {
            // for delete
            return this.executeDelete((SQLDeleteStatement) sqlStatement, mongoDatabase);
        }
        throw new IllegalArgumentException("illegal mongo context for execution.");
    }

    /**
     * transform sql and execute
     *
     * @param sqlSelectStatement      sql select statement
     * @param mongoDatabase           mongo database
     * @param aggregationAllowDiskUse whether disk using is allowed whiling aggregating
     * @param aggregationBatchSize    batch size of aggregation
     * @return execute result
     */
    JsonNode executeSelect(SQLSelectStatement sqlSelectStatement, MongoDatabase mongoDatabase, boolean aggregationAllowDiskUse, int aggregationBatchSize);

    /**
     * transform sql and execute
     *
     * @param sqlInsertStatement sql insert statement
     * @param mongoDatabase      mongo database
     * @return execute result
     */
    default JsonNode executeInsert(SQLInsertStatement sqlInsertStatement, MongoDatabase mongoDatabase) {
        this.visit(sqlInsertStatement);
        SQLExprTableSource tableSource = sqlInsertStatement.getTableSource();
        String tableName = tableSource.getTableName();
        List<String> insertColumns = sqlInsertStatement.getColumns().stream().map(sqlExpr -> ((SQLName) sqlExpr).getSimpleName()).collect(Collectors.toList());
        List<SQLInsertStatement.ValuesClause> valuesClauseList = sqlInsertStatement.getValuesList();
        List<Document> generatedDocumentList = new ArrayList<>();
        for (SQLInsertStatement.ValuesClause clause : valuesClauseList) {
            List<SQLExpr> values = clause.getValues();
            Document document = new Document();
            for (int i = 0; i < values.size(); i++) {
                document.put(insertColumns.get(i), SqlUtils.getRealValue(values.get(i)));
            }
            generatedDocumentList.add(document);
        }
        InsertManyResult insertManyResult = mongoDatabase.getCollection(tableName).insertMany(generatedDocumentList);
        return new LongNode(insertManyResult.wasAcknowledged() ? insertManyResult.getInsertedIds().size() : 0L);
    }

    /**
     * transform sql and execute
     *
     * @param sqlUpdateStatement sql update statement
     * @param mongoDatabase      mongo database
     * @return execute result
     */
    default JsonNode executeUpdate(SQLUpdateStatement sqlUpdateStatement, MongoDatabase mongoDatabase) {
        this.visit(sqlUpdateStatement);
        SQLExprTableSource tableSource = (SQLExprTableSource) sqlUpdateStatement.getTableSource();
        String tableName = tableSource.getTableName();
        SQLExpr whereClause = sqlUpdateStatement.getWhere();
        List<SQLUpdateSetItem> items = sqlUpdateStatement.getItems();
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
        Document where = this.getWhere(whereClause);
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

    /**
     * transform sql and execute
     *
     * @param sqlDeleteStatement sql delete statement
     * @param mongoDatabase      mongo database
     * @return execute result
     */
    default JsonNode executeDelete(SQLDeleteStatement sqlDeleteStatement, MongoDatabase mongoDatabase) {
        this.visit(sqlDeleteStatement);
        String tableName = sqlDeleteStatement.getTableName().getSimpleName();
        SQLExpr where = sqlDeleteStatement.getWhere();
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(tableName);
        DeleteResult deleteResult = mongoCollection.deleteMany(this.getWhere(where));
        return new LongNode(deleteResult.wasAcknowledged() ? deleteResult.getDeletedCount() : 0L);
    }
}
