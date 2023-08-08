package com.github.xiao808.mongo.sql;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.xiao808.mongo.sql.codec.BsonObjectNodeCodecProvider;
import com.github.xiao808.mongo.sql.visitor.SqlVisitor;
import com.github.xiao808.mongo.sql.visitor.SqlVisitorFactory;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.codecs.BsonValueCodecProvider;
import org.bson.codecs.DocumentCodecProvider;
import org.bson.codecs.IterableCodecProvider;
import org.bson.codecs.MapCodecProvider;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.internal.CodecRegistryHelper;

import java.util.Objects;

import static java.util.Arrays.asList;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;

/**
 * sql -> mongo bson converter
 *
 * @author zengxiao
 * @date 2023/3/22 11:47
 * @since 1.0
 **/
@Slf4j
public final class MongoSqlExecutor {

    /**
     * sql statement parsed by druid
     */
    private final SQLStatement sqlStatement;

    /**
     * whether disk can be used while aggregate.
     */
    private final boolean aggregationAllowDiskUse;

    /**
     * batch row of aggregation.
     */
    private final int aggregationBatchSize;

    /**
     * codec support for jackson
     */
    private static final CodecRegistry JACKSON_JSON_NODE_CODEC_REGISTRY = CodecRegistryHelper.createRegistry(
            fromProviders(asList(
                    new ValueCodecProvider(),
                    new BsonValueCodecProvider(),
                    new DocumentCodecProvider(),
                    new MapCodecProvider(),
                    new BsonObjectNodeCodecProvider(),
                    new IterableCodecProvider())),
            UuidRepresentation.STANDARD);

    /**
     * transform and execute sql.
     * <p>
     * usage:
     * new MongoSqlExecutor("select * ...",
     * DbType.mysql,
     * true,
     * 2000
     * )
     * .execute(mongoDatabase);
     *
     * @param sql                     sql to parse
     * @param dbType                  database type
     * @param aggregationAllowDiskUse set whether disk use is allowed during aggregation
     * @param aggregationBatchSize    set the batch size for aggregation
     */
    public MongoSqlExecutor(final String sql,
                            final DbType dbType,
                            final boolean aggregationAllowDiskUse,
                            final int aggregationBatchSize) {
        // empty sql is not permitted.
        if (StringUtils.isEmpty(sql)) {
            throw new IllegalArgumentException("sql to transform can not be null.");
        }
        // avoid negative number.
        int actualAggregationBatchSize = aggregationBatchSize;
        if (actualAggregationBatchSize < 0) {
            actualAggregationBatchSize = 1000;
        }
        this.sqlStatement = SQLUtils.parseSingleStatement(sql, dbType);
        this.aggregationAllowDiskUse = aggregationAllowDiskUse;
        this.aggregationBatchSize = actualAggregationBatchSize;
    }

    /**
     * execute sql with target mongo database
     *
     * @param mongoDatabase mongo database
     * @return result of sql execution
     */
    public JsonNode execute(MongoDatabase mongoDatabase) {
        return this.execute(mongoDatabase, JACKSON_JSON_NODE_CODEC_REGISTRY);
    }

    /**
     * execute sql with target mongo database
     *
     * @param mongoDatabase mongo database
     * @param codecRegistry codec for execution result
     * @return result of sql execution
     */
    public JsonNode execute(MongoDatabase mongoDatabase, CodecRegistry codecRegistry) {
        String version = mongoDatabase
                .runCommand(new BsonDocument("buildinfo", new BsonString("")))
                .get("version")
                .toString();
        SqlVisitor sqlVisitor = SqlVisitorFactory.create(version);
        Objects.requireNonNull(sqlStatement, "sql visitor is required.");
        try {
            // communicate with mongodb and gain the result.
            return sqlVisitor.execute(sqlStatement, mongoDatabase.withCodecRegistry(codecRegistry), aggregationAllowDiskUse, aggregationBatchSize);
        } catch (Exception e) {
            log.error("parse sql and execute exceptionally", e);
            throw new RuntimeException(e);
        } finally {
            Document document = sqlVisitor.getDocument(sqlStatement);
            if (!document.isEmpty()) {
                log.info(document.toJson());
            }
        }
    }
}
