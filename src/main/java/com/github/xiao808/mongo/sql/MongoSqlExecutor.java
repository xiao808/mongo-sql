package com.github.xiao808.mongo.sql;

import com.alibaba.druid.DbType;
import com.alibaba.druid.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.xiao808.mongo.sql.codec.BsonObjectNodeCodecProvider;
import com.github.xiao808.mongo.sql.execution.SqlExecution;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.internal.MongoDatabaseImpl;
import com.mongodb.client.internal.OperationExecutor;
import com.mongodb.internal.operation.ReadOperation;
import com.mongodb.internal.operation.WriteOperation;
import org.bson.UuidRepresentation;
import org.bson.codecs.BsonValueCodecProvider;
import org.bson.codecs.DocumentCodecProvider;
import org.bson.codecs.IterableCodecProvider;
import org.bson.codecs.MapCodecProvider;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.internal.CodecRegistryHelper;

import static java.util.Arrays.asList;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;

/**
 * @author zengxiao
 * @description sql -> mongo bson converter
 * @date 2023/3/22 11:47
 * @since 1.0
 **/
public final class MongoSqlExecutor {

    private static final CodecRegistry JACKSON_JSON_NODE_CODEC_REGISTRY = CodecRegistryHelper.createRegistry(
            fromProviders(asList(new ValueCodecProvider(), new IterableCodecProvider(),
                    new BsonValueCodecProvider(), new DocumentCodecProvider(), new MapCodecProvider(), new BsonObjectNodeCodecProvider())),
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
        try {
            // initialize converter for current execution.
            QueryTransformer context = new QueryTransformer.Builder()
                    .sql(sql)
                    .dbType(dbType)
                    .aggregationBatchSize(actualAggregationBatchSize)
                    .aggregationAllowDiskUse(aggregationAllowDiskUse)
                    .build();
            InheritableThreadLocalMongoContextHolder.setContext(context);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public JsonNode execute(MongoDatabase mongoDatabase) {
        // get sql execution
        SqlExecution execution = SqlExecution.getInstance();
        // communicate with mongodb and gain the result.
        return execution.execute(mongoDatabase.withCodecRegistry(JACKSON_JSON_NODE_CODEC_REGISTRY));
    }

    public static void main(String[] args) {
        MongoDatabaseImpl mongoDatabase = new MongoDatabaseImpl(
                "test",
                JACKSON_JSON_NODE_CODEC_REGISTRY,
                ReadPreference.primary(),
                WriteConcern.ACKNOWLEDGED,
                false,
                false,
                ReadConcern.DEFAULT,
                UuidRepresentation.STANDARD,
                new OperationExecutor() {
                    @Override
                    public <T> T execute(ReadOperation<T> operation, ReadPreference readPreference, ReadConcern readConcern) {
                        return null;
                    }

                    @Override
                    public <T> T execute(WriteOperation<T> operation, ReadConcern readConcern) {
                        return null;
                    }

                    @Override
                    public <T> T execute(ReadOperation<T> operation, ReadPreference readPreference, ReadConcern readConcern, ClientSession session) {
                        return null;
                    }

                    @Override
                    public <T> T execute(WriteOperation<T> operation, ReadConcern readConcern, ClientSession session) {
                        return null;
                    }
                }
        );
//        String select = "select distinct(e.id), s.id, count(*) as cn from (select * from student where exam_id = 1) s left join exam e on s.exam_id = e.id where e.id is not null and name not like '%123%' group by e.id, s.id order by cn desc limit 10, 10";
//        new MongoSqlExecutor(select, DbType.mysql, true, 2000).execute(mongoDatabase);
//        String update = "update student set seat_no = 1, name = 1 where id = 1";
//        new MongoSqlExecutor(update, DbType.mysql, true, 2000).execute(mongoDatabase);
        String insert = "insert into exam (id, name, age) values (1, 1, 1), (2, 2, 2)";
        new MongoSqlExecutor(insert, DbType.mysql, true, 2000).execute(mongoDatabase);
        String delete = "delete from exam where id = 1 and age = 1";
        new MongoSqlExecutor(delete, DbType.mysql, true, 2000).execute(mongoDatabase);
    }
}
