package com.github.xiao808.mongo.sql.execution;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.github.xiao808.mongo.sql.utils.SqlUtils;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertManyResult;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * insert action for mongo
 * simple insert sql clause can be parsed
 * eg: insert into table (id, age, name, sex) values (...), (...)...
 *
 * @author zengxiao
 * @date 2023/3/22 16:24
 * @since 1.0
 **/
@Slf4j
public class SqlInsertExecution extends AbstractSqlExecution {

    public SqlInsertExecution(SQLInsertStatement sqlInsertStatement) {
        super(sqlInsertStatement);
    }

    @Override
    public JsonNode execute(MongoDatabase mongoDatabase) {
        SQLInsertStatement insertStatement = (SQLInsertStatement) this.sqlStatement;
        sqlStatementTransformVisitor.visit(insertStatement);
        SQLExprTableSource tableSource = insertStatement.getTableSource();
        String tableName = tableSource.getTableName();
        List<String> insertColumns = insertStatement.getColumns().stream().map(sqlExpr -> ((SQLName) sqlExpr).getSimpleName()).collect(Collectors.toList());
        List<SQLInsertStatement.ValuesClause> valuesClauseList = insertStatement.getValuesList();
        InsertManyResult insertManyResult = mongoDatabase.getCollection(tableName).insertMany(generateInsertDocument(insertColumns, valuesClauseList));
        return new LongNode(insertManyResult.wasAcknowledged() ? insertManyResult.getInsertedIds().size() : 0L);
    }

    private List<Document> generateInsertDocument(List<String> columns, List<SQLInsertStatement.ValuesClause> valuesClauseList) {
        List<Document> result = new ArrayList<>();
        for (SQLInsertStatement.ValuesClause clause : valuesClauseList) {
            List<SQLExpr> values = clause.getValues();
            Document document = new Document();
            for (int i = 0; i < values.size(); i++) {
                document.put(columns.get(i), SqlUtils.getRealValue(values.get(i)));
            }
            result.add(document);
        }
        return result;
    }
}
