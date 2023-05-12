package com.github.xiao808.mongo.sql.execution;

import com.alibaba.druid.sql.ast.SQLLimit;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.xiao808.mongo.sql.utils.SqlUtils;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.github.xiao808.mongo.sql.LexerConstants.GROUP;
import static com.github.xiao808.mongo.sql.LexerConstants.LIMIT;
import static com.github.xiao808.mongo.sql.LexerConstants.MATCH;
import static com.github.xiao808.mongo.sql.LexerConstants.PROJECT;
import static com.github.xiao808.mongo.sql.LexerConstants.ROOT;
import static com.github.xiao808.mongo.sql.LexerConstants.SKIP;
import static com.github.xiao808.mongo.sql.LexerConstants.SUM;
import static com.github.xiao808.mongo.sql.MongoIdConstants.DOT;
import static com.github.xiao808.mongo.sql.MongoIdConstants.EMPTY_STRING;
import static com.github.xiao808.mongo.sql.MongoIdConstants.MONGO_ID;
import static com.github.xiao808.mongo.sql.MongoIdConstants.REPRESENT_PAGE_TOTAL;

/**
 * select action for mongo
 * support part of ANSI SQL
 * 1.table join (just support left out join and inner join now)
 * 2.group、having
 * 3.sub query
 * 4.some aggregate function
 *
 * @author zengxiao
 * @date 2023/3/22 15:44
 * @see com.github.xiao808.mongo.sql.AggregateEnum
 * @see com.github.xiao808.mongo.sql.LexerConstants
 * @since 1.0
 **/
public class SqlSelectExecution extends AbstractSqlExecution {


    /**
     * whether disk can be used while aggregate
     */
    private final boolean aggregationAllowDiskUse;
    /**
     * batch row of aggregation.
     */
    private final int aggregationBatchSize;

    public SqlSelectExecution(SQLSelectStatement sqlSelectStatement, boolean aggregationAllowDiskUse, int aggregationBatchSize) {
        super(sqlSelectStatement);
        this.aggregationAllowDiskUse = aggregationAllowDiskUse;
        this.aggregationBatchSize = aggregationBatchSize;
    }

    @Override
    public JsonNode execute(MongoDatabase mongoDatabase) {
        SQLSelectStatement selectStatement = (SQLSelectStatement) this.sqlStatement;
        selectStatement.accept(sqlStatementTransformVisitor);
        String tableName = sqlStatementTransformVisitor.getAggregationTableName(selectStatement);
        MongoCollection<ObjectNode> collection = mongoDatabase.getCollection(tableName, ObjectNode.class);
        if (sqlStatementTransformVisitor.isDistinct()) {
            // select distinct
            return new DistinctOperation().execute(selectStatement, collection);
        } else if (sqlStatementTransformVisitor.isCountAll() && !sqlStatementTransformVisitor.isAggregate()) {
            // select count(*)
            return new CountOperation().execute(selectStatement, collection);
        } else {
            // aggregation
            return new AggregateOperation().execute(selectStatement, collection);
        }
    }

    /**
     * distinct
     */
    class DistinctOperation {

        public JsonNode execute(SQLSelectStatement selectStatement, MongoCollection<ObjectNode> collection) {
            SQLSelectQueryBlock queryBlock = selectStatement.getSelect().getQueryBlock();
            String tableAlias = queryBlock.getFrom().getAlias();
            Document filter = sqlStatementTransformVisitor.getWhere(queryBlock.getWhere());
            String distinctField = sqlStatementTransformVisitor.getDistinctField().replace(tableAlias + DOT, EMPTY_STRING);
            if (filter != null && !filter.isEmpty()) {
                filter = Document.parse(filter.toJson().replace(tableAlias + DOT, EMPTY_STRING));
            }
            DistinctIterable<String> distinctIterable = collection.distinct(distinctField, filter, String.class);
            ArrayNode result = new ArrayNode(JsonNodeFactory.instance);
            for (String data : distinctIterable) {
                result.add(data);
            }
            return result;
        }
    }

    /**
     * aggregation
     */
    class AggregateOperation {

        public JsonNode execute(SQLSelectStatement selectStatement, MongoCollection<ObjectNode> collection) {
            SQLSelectQueryBlock queryBlock = selectStatement.getSelect().getQueryBlock();
            SQLLimit limit = queryBlock.getLimit();
            List<Document> aggregation = sqlStatementTransformVisitor.getAggregation(selectStatement);
            AggregateIterable<ObjectNode> aggregate = collection.aggregate(aggregation)
                    .allowDiskUse(aggregationAllowDiskUse)
                    .batchSize(aggregationBatchSize);
            return Objects.nonNull(limit) ? unwrapPaginationResult(aggregate) : unwrapResult(aggregate);
        }

        /**
         * unwrap result
         *
         * @param aggregate aggregation result iterable
         * @return aggregate result
         */
        private ArrayNode unwrapResult(AggregateIterable<ObjectNode> aggregate) {
            ArrayNode result = new ArrayNode(JsonNodeFactory.instance);
            for (ObjectNode node : aggregate) {
                result.add(node);
            }
            return result;
        }

        /**
         * unwrap result
         *
         * @param aggregate aggregation result iterable
         * @return aggregate result
         */
        private ObjectNode unwrapPaginationResult(AggregateIterable<ObjectNode> aggregate) {
            try (MongoCursor<ObjectNode> cursor = aggregate.iterator()) {
                return cursor.hasNext() ? cursor.next() : new ObjectNode(JsonNodeFactory.instance);
            }
        }
    }

    /**
     * select count(*)
     */
    class CountOperation {

        public LongNode execute(SQLSelectStatement selectStatement, MongoCollection<ObjectNode> collection) {
            SQLSelectQueryBlock queryBlock = selectStatement.getSelect().getQueryBlock();
            String alias = queryBlock.getFrom().getAlias();
            SQLLimit limit = queryBlock.getLimit();
            List<Document> aggregation = new ArrayList<>();
            if (!StringUtils.isEmpty(alias)) {
                aggregation.add(new Document(PROJECT, new Document(Map.of(alias, ROOT, MONGO_ID, 0))));
            }
            Document filter = sqlStatementTransformVisitor.getWhere(queryBlock.getWhere());
            aggregation.add(new Document(MATCH, filter != null ? filter : new BsonDocument()));
            if (Objects.nonNull(limit)) {
                Long skip = (Long) SqlUtils.getRealValue(limit.getOffset());
                Long size = (Long) SqlUtils.getRealValue(limit.getRowCount());
                if (Objects.nonNull(skip) && skip > 0) {
                    aggregation.add(new Document(SKIP, new BsonInt64(skip)));
                }
                if (Objects.nonNull(size) && size > 0) {
                    aggregation.add(new Document(LIMIT, new BsonInt64(size)));
                }
            }
            aggregation.add(new Document(GROUP, new Document(MONGO_ID, new BsonInt32(1))
                    .append(REPRESENT_PAGE_TOTAL, new Document(SUM, new BsonInt32(1)))));
            try (MongoCursor<ObjectNode> cursor = collection.aggregate(aggregation)
                    .allowDiskUse(aggregationAllowDiskUse)
                    .batchSize(aggregationBatchSize)
                    .iterator()) {
                return new LongNode(cursor.hasNext() ? unwrapResult(cursor.next()) : 0);
            }
        }

        /**
         * unwrap result
         *
         * @param result aggregation result
         * @return count result
         */
        private Long unwrapResult(final ObjectNode result) {
            if (result == null || result.isEmpty()) {
                return 0L;
            } else {
                return result.get(REPRESENT_PAGE_TOTAL).longValue();
            }
        }
    }
}
