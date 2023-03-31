package com.github.xiao808.mongo.sql.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.xiao808.mongo.sql.InheritableThreadLocalMongoContextHolder;
import com.github.xiao808.mongo.sql.MongoDBQueryHolder;
import com.github.xiao808.mongo.sql.QueryTransformer;
import com.github.xiao808.mongo.sql.holder.from.SQLCommandInfoHolder;
import com.github.xiao808.mongo.sql.utils.SqlUtils;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

/**
 * @author zengxiao
 * @description select action for mongo
 * @date 2023/3/22 15:44
 * @since 1.0
 **/
public class SqlSelectExecution implements SqlExecution {

    @Override
    public JsonNode execute(MongoDatabase mongoDatabase) {
        QueryTransformer mongoContext = InheritableThreadLocalMongoContextHolder.getContext();
        MongoDBQueryHolder queryHolder = mongoContext.getDBMongoQueryHolder();
        SQLCommandInfoHolder sqlCommandInfoHolder = mongoContext.getSqlCommandInfoHolder();
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(queryHolder.getCollection());
        if (queryHolder.isDistinct()) {
            // select distinct
            ArrayNode result = new ArrayNode(JsonNodeFactory.instance);
            DistinctIterable<ObjectNode> distinct = mongoCollection.distinct(getDistinctFieldName(queryHolder), queryHolder.getQuery(), ObjectNode.class);
            for (JsonNode jsonNode : distinct) {
                result.add(jsonNode);
            }
            return result;
        } else if (sqlCommandInfoHolder.isCountAll() && !isAggregate(queryHolder, sqlCommandInfoHolder)) {
            // select count(*)
            return new LongNode(mongoCollection.countDocuments(queryHolder.getQuery()));
        } else if (isAggregate(queryHolder, sqlCommandInfoHolder)) {
            // configuration for Insufficient memory when computing large amounts of data
            Boolean aggregationAllowDiskUse = mongoContext.getAggregationAllowDiskUse();
            Integer aggregationBatchSize = mongoContext.getAggregationBatchSize();
            // for sub query、join、group
            // select ... from ... left join ...
            // select ... from ... group by
            AggregateIterable<ObjectNode> aggregate = mongoCollection.aggregate(mongoContext.generateAggSteps(queryHolder, sqlCommandInfoHolder), ObjectNode.class);
            if (aggregationAllowDiskUse != null) {
                aggregate.allowDiskUse(aggregationAllowDiskUse);
            }
            if (aggregationBatchSize != null) {
                aggregate.batchSize(aggregationBatchSize);
            }
            ArrayNode result = new ArrayNode(JsonNodeFactory.instance);
            for (JsonNode jsonNode : aggregate) {
                result.add(jsonNode);
            }
            return result;
        } else {
            // for single table query
            // select ... from ... where ....
            FindIterable<ObjectNode> findIterable = mongoCollection.find(queryHolder.getQuery(), ObjectNode.class)
                    .projection(queryHolder.getProjection());
            if (queryHolder.getSort() != null && queryHolder.getSort().size() > 0) {
                findIterable.sort(queryHolder.getSort());
            }
            if (queryHolder.getOffset() != -1) {
                findIterable.skip((int) queryHolder.getOffset());
            }
            if (queryHolder.getLimit() != -1) {
                findIterable.limit((int) queryHolder.getLimit());
            }
            ArrayNode result = new ArrayNode(JsonNodeFactory.instance);
            for (JsonNode jsonNode : findIterable) {
                result.add(jsonNode);
            }
            return result;
        }
    }

    /**
     * whether sql is aggregation
     *
     * @param mongoQueryHolder     mongo query holder
     * @param sqlCommandInfoHolder sql command info holder
     * @return if the sql is aggregation
     */
    private boolean isAggregate(final MongoDBQueryHolder mongoQueryHolder, final SQLCommandInfoHolder sqlCommandInfoHolder) {
        return (sqlCommandInfoHolder.getAliasHolder() != null
                && !sqlCommandInfoHolder.getAliasHolder().isEmpty())
                || sqlCommandInfoHolder.getGroupBys().size() > 0
                || (sqlCommandInfoHolder.getJoins() != null && sqlCommandInfoHolder.getJoins().size() > 0)
                || (mongoQueryHolder.getPrevSteps() != null && !mongoQueryHolder.getPrevSteps().isEmpty())
                || (sqlCommandInfoHolder.isTotalGroup() && !SqlUtils.isCountAll(sqlCommandInfoHolder.getSelectItems()));
    }

    /**
     * get distinct column name
     *
     * @param mongoQueryHolder mongo query holder
     * @return distinct column name
     */
    private String getDistinctFieldName(final MongoDBQueryHolder mongoQueryHolder) {
        return mongoQueryHolder.getProjection().keySet().iterator().next();
    }
}
