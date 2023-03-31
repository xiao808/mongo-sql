package com.github.xiao808.mongo.sql.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.github.xiao808.mongo.sql.InheritableThreadLocalMongoContextHolder;
import com.github.xiao808.mongo.sql.MongoDBQueryHolder;
import com.github.xiao808.mongo.sql.QueryTransformer;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;

/**
 * @author zengxiao
 * @description delete action for mongo
 * @date 2023/3/22 16:25
 * @since 1.0
 **/
public class SqlDeleteExecution implements SqlExecution {

    @Override
    public JsonNode execute(MongoDatabase mongoDatabase) {
        QueryTransformer mongoContext = InheritableThreadLocalMongoContextHolder.getContext();
        MongoDBQueryHolder queryHolder = mongoContext.getDBMongoQueryHolder();
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(queryHolder.getCollection());
        DeleteResult deleteResult = mongoCollection.deleteMany(queryHolder.getQuery());
        return new LongNode(deleteResult.wasAcknowledged() ? deleteResult.getDeletedCount() : 0L);
    }
}
