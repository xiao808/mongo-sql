package com.github.xiao808.mongo.sql.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.github.xiao808.mongo.sql.InheritableThreadLocalMongoContextHolder;
import com.github.xiao808.mongo.sql.MongoDBQueryHolder;
import com.github.xiao808.mongo.sql.QueryTransformer;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author zengxiao
 * @description update action for mongo
 * @date 2023/3/22 16:24
 * @since 1.0
 **/
public class SqlUpdateExecution implements SqlExecution {

    @Override
    public JsonNode execute(MongoDatabase mongoDatabase) {
        QueryTransformer mongoContext = InheritableThreadLocalMongoContextHolder.getContext();
        MongoDBQueryHolder queryHolder = mongoContext.getDBMongoQueryHolder();
        Document updateSet = queryHolder.getUpdateSet();
        List<String> fieldsToUnset = queryHolder.getFieldsToUnset();
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(queryHolder.getCollection());
        UpdateResult updateResult = null;
        if ((updateSet != null && !updateSet.isEmpty()) && (fieldsToUnset != null && !fieldsToUnset.isEmpty())) {
            updateResult = mongoCollection.updateMany(queryHolder.getQuery(),
                    Arrays.asList(new Document().append("$set", updateSet),
                            new Document().append("$unset", fieldsToUnset)));
        } else if (updateSet != null && !updateSet.isEmpty()) {
            updateResult = mongoCollection.updateMany(queryHolder.getQuery(),
                    new Document().append("$set", updateSet));
        } else if (fieldsToUnset != null && !fieldsToUnset.isEmpty()) {
            updateResult = mongoCollection.updateMany(queryHolder.getQuery(),
                    new Document().append("$unset", fieldsToUnset));
        }
        UpdateResult result = Optional.ofNullable(updateResult).orElseGet(() -> UpdateResult.acknowledged(0, 0L, null));
        return new LongNode(result.wasAcknowledged() ? result.getModifiedCount() : 0L);
    }
}
