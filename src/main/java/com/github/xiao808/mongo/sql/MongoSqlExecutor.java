package com.github.xiao808.mongo.sql;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.parser.SQLParserFeature;
import com.alibaba.druid.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.xiao808.mongo.sql.execution.SqlExecution;
import com.github.xiao808.mongo.sql.visitor.SqlTransformFromMysqlToMongoVisitor;
import com.github.xiao808.mongo.sql.visitor.SqlTransformToMongoVisitor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.internal.MongoDatabaseImpl;
import org.bson.Document;

import java.util.Objects;

/**
 * @author zengxiao
 * @description sql -> mongo bson converter
 * @date 2023/3/22 11:47
 * @since 1.0
 **/
public final class MongoSqlExecutor {

    private final SqlTransformToMongoVisitor transformVisitor;

    /**
     * transform and execute sql.
     *
     * usage:
     * new MongoSqlExecutor("select * ...",
     *                  new SqlTransformFromMysqlToMongoVisitor(),
     *                  true,
     *                  2000
     *              )
     *              .execute(mongoDatabase);
     *
     *
     * @param sql                     sql to parse
     * @param sqlTransformVisitor     sql transform visitor
     * @param aggregationAllowDiskUse set whether disk use is allowed during aggregation
     * @param aggregationBatchSize    set the batch size for aggregation
     */
    private MongoSqlExecutor(final String sql,
                             final SqlTransformToMongoVisitor sqlTransformVisitor,
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
        // use druid parse sql in special database sql type.
        // not support multi statement.
        SQLStatement sqlStatement = SQLUtils.parseSingleStatement(sql, DbType.of(sqlTransformVisitor.getSrcDbType()), SQLParserFeature.PrintSQLWhileParsingFailed, SQLParserFeature.IgnoreNameQuotes);
        // sql transformer actually.
        this.transformVisitor = Objects.requireNonNullElseGet(sqlTransformVisitor, SqlTransformFromMysqlToMongoVisitor::new);
        // initialize context for current execution.
        MongoContext context = MongoContext.builder()
                .sqlStatement(sqlStatement)
                .aggregationBatchSize(actualAggregationBatchSize)
                .aggregationAllowDiskUse(aggregationAllowDiskUse)
                .condition(new Document())
                .selectColumn(new Document())
                .build();
        InheritableThreadLocalMongoContextHolder.setContext(context);
    }

    public JsonNode execute(MongoDatabase mongoDatabase) {
        // get sql execution
        SqlExecution execution = SqlExecution.getInstance();
        execution.accept(transformVisitor);
        // communicate with mongodb and gain the result.
        return execution.execute(mongoDatabase);
    }

    public static void main(String[] args) {
        String select = "select\n" +
                "\ta.questionid,\n" +
                "\ta.questioncontent,\n" +
                "\ta.rightoptnum,\n" +
                "\ta.rightoptid,\n" +
                "\tb.stuoptnum,\n" +
                "\tb.stuoptionid\n" +
                "from \n" +
                "\t\t (\n" +
                "\tselect\n" +
                "\t\ttq1.questionid,\n" +
                "\t\ttq1.questioncontent,\n" +
                "\t\tGROUP_CONCAT(teod1.optionnum order by teod1.optionnum separator '') as rightoptnum ,\n" +
                "\t\tteod1.optionid as rightoptid\n" +
                "\tfrom\n" +
                "\t\ttbexamquestion tq1 ,\n" +
                "\t\ttbexamoptionandanswer teod1,\n" +
                "\t\ttbexampaperquestion tepq1,\n" +
                "\t\ttbexamandpaper teap1\n" +
                "\twhere\n" +
                "\t\ttq1.questionid = teod1.questionid\n" +
                "\t\tand teod1.isanswer = 1\n" +
                "\t\tand tq1.questionid = tepq1.questionid\n" +
                "\t\tand tepq1.paperid = teap1.paperid\n" +
                "\t\tand teap1.examid = 1110\n" +
                "\tgroup by\n" +
                "\t\tquestionid) a\n" +
                "left join \n" +
                "\t\t  (\n" +
                "\tselect\n" +
                "\t\ttq.questionid,\n" +
                "\t\ttepa.userid,\n" +
                "\t\tGROUP_CONCAT(teod.optionnum order by teod.optionnum separator '') as stuoptnum,\n" +
                "\t\tteod.optionid as stuoptionid\n" +
                "\tfrom\n" +
                "\t\ttbexampaperanswer tepa,\n" +
                "\t\ttbexamquestion tq ,\n" +
                "\t\ttbexamoptionandanswer teod\n" +
                "\twhere\n" +
                "\t\ttepa.questionid = tq.questionid\n" +
                "\t\tand tepa.optionid = teod.optionid\n" +
                "\t\tand tq.questionid = teod.questionid\n" +
                "\t\tand tepa.examid = 1110\n" +
                "\t\tand tepa.userid = 123\n" +
                "\tgroup by\n" +
                "\t\tquestionid) b on\n" +
                "\ta.questionid = b.questionid";
        MongoSqlExecutor executor = new Builder()
                .sql(select)
                .aggregationAllowDiskUse(true)
                .aggregationBatchSize(2000)
                .sqlTransformVisitor(new SqlTransformFromMysqlToMongoVisitor())
                .build();
        JsonNode execute = executor.execute(null);
    }

    /**
     * Builder for SqlConverter
     */
    public static class Builder {

        /**
         * Sets the number of documents to return per batch.
         */
        private Boolean aggregationAllowDiskUse = null;

        /**
         * whether disk use is allowed for aggregation.
         */
        private Integer aggregationBatchSize = null;

        /**
         * SQLStatement list using druid SQLStatementParser.
         */
        private String sql;

        /**
         * sql transform visitor
         */
        private SqlTransformToMongoVisitor sqlTransformVisitor;

        /**
         * set the sql string.
         *
         * @param sql the sql
         * @return the builder
         */
        public Builder sql(final String sql) {
            Objects.requireNonNull(sql);
            this.sql = sql;
            return this;
        }

        /**
         * set the sql transform visitor.
         *
         * @param sqlTransformVisitor the sql transform visitor
         * @return the builder
         */
        public Builder sqlTransformVisitor(final SqlTransformToMongoVisitor sqlTransformVisitor) {
            this.sqlTransformVisitor = sqlTransformVisitor;
            return this;
        }

        /**
         * set whether aggregation is allowed to use disk use.
         *
         * @param aggregationAllowDiskUse set to true to allow disk use during aggregation
         * @return the builder
         */
        public Builder aggregationAllowDiskUse(final Boolean aggregationAllowDiskUse) {
            Objects.requireNonNull(aggregationAllowDiskUse);
            this.aggregationAllowDiskUse = aggregationAllowDiskUse;
            return this;
        }

        /**
         * set the batch size for aggregation.
         *
         * @param aggregationBatchSize the batch size option to use for aggregation
         * @return the builder
         */
        public Builder aggregationBatchSize(final Integer aggregationBatchSize) {
            Objects.requireNonNull(aggregationBatchSize);
            this.aggregationBatchSize = aggregationBatchSize;
            return this;
        }

        /**
         * build the {@link MongoSqlExecutor}.
         *
         * @return the {@link MongoSqlExecutor}
         */
        public MongoSqlExecutor build() {
            return new MongoSqlExecutor(sql, sqlTransformVisitor, aggregationAllowDiskUse, aggregationBatchSize);
        }
    }
}
