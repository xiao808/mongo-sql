package com.github.xiao808.mongo.sql.execution;

import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.xiao808.mongo.sql.InheritableThreadLocalMongoContextHolder;
import com.github.xiao808.mongo.sql.MongoContext;
import com.github.xiao808.mongo.sql.ParseException;
import com.github.xiao808.mongo.sql.QueryConverter;
import com.mongodb.client.MongoDatabase;

/**
 * @author zengxiao
 * @description
 * @date 2023/3/22 15:44
 * @since 1.0
 **/
public class SqlSelectExecution implements SqlExecution {

    @Override
    public JsonNode execute(MongoDatabase mongoDatabase) {
        MongoContext mongoContext = InheritableThreadLocalMongoContextHolder.getContext();
        SQLSelectStatement sqlStatement = (SQLSelectStatement) mongoContext.getSqlStatement();
        System.err.println("select.......................");
        String select = "select e.id, s.id, count(*) as cn from (select * from student where exam_id = 1) s left join exam e on s.exam_id = e.id where e.id is null and s.name not like '%123%' group by e.id, s.id order by cn desc limit 10, 10";
        try {
            QueryConverter build = new QueryConverter.Builder().aggregationAllowDiskUse(true)
                    .aggregationBatchSize(2000)
                    .sql(select)
                    .build();
            System.err.println(build.getQueryAsDocument().toJson());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }
}
