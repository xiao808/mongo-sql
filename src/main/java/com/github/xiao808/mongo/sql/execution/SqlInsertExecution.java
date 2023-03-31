package com.github.xiao808.mongo.sql.execution;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.github.xiao808.mongo.sql.FieldType;
import com.github.xiao808.mongo.sql.InheritableThreadLocalMongoContextHolder;
import com.github.xiao808.mongo.sql.ParseException;
import com.github.xiao808.mongo.sql.QueryTransformer;
import com.github.xiao808.mongo.sql.holder.from.SQLCommandInfoHolder;
import com.github.xiao808.mongo.sql.utils.SqlUtils;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertManyResult;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zengxiao
 * @description insert action for mongo
 * @date 2023/3/22 16:24
 * @since 1.0
 **/
@Slf4j
public class SqlInsertExecution implements SqlExecution {

    @Override
    public JsonNode execute(MongoDatabase mongoDatabase) {
        System.err.println("insert.......................");
        QueryTransformer mongoContext = InheritableThreadLocalMongoContextHolder.getContext();
        SQLCommandInfoHolder sqlCommandInfoHolder = mongoContext.getSqlCommandInfoHolder();
        String baseFromTableName = sqlCommandInfoHolder.getFromHolder().getBaseFromTableName();
        List<String> insertColumns = sqlCommandInfoHolder.getInsertColumns();
        List<SQLInsertStatement.ValuesClause> valuesClauseList = sqlCommandInfoHolder.getValuesClauseList();
        InsertManyResult insertManyResult = mongoDatabase.getCollection(baseFromTableName).insertMany(generateInsertDocument(insertColumns, valuesClauseList));
        return new LongNode(insertManyResult.wasAcknowledged() ? insertManyResult.getInsertedIds().size() : 0L);
    }

    private List<Document> generateInsertDocument(List<String> columns, List<SQLInsertStatement.ValuesClause> valuesClauseList) {
        List<Document> result = new ArrayList<>();
        for (SQLInsertStatement.ValuesClause clause : valuesClauseList) {
            List<SQLExpr> values = clause.getValues();
            Document document = new Document();
            for (int i = 0; i < values.size(); i++) {
                try {
                    document.put(columns.get(i), SqlUtils.getNormalizedValue(values.get(i), null, FieldType.STRING, null, null, null));
                } catch (ParseException e) {
                    log.error("generate insert document exceptionally.", e);
                }
            }
            result.add(document);
        }
        return result;
    }
}
