package com.github.xiao808.mongo.sql;

import com.alibaba.druid.sql.ast.SQLStatement;
import lombok.Builder;
import lombok.Getter;
import org.bson.Document;

/**
 * @author zengxiao
 * @description
 * @date 2023/3/22 14:59
 * @since 1.0
 **/
@Getter
@Builder
public class MongoContext {

    /**
     * Sets the number of documents to return per batch.
     */
    private Integer aggregationBatchSize;

    /**
     * whether disk use is allowed for aggregation.
     */
    private Boolean aggregationAllowDiskUse;

    /**
     * SQLStatement using druid SQLStatementParser.
     */
    private SQLStatement sqlStatement;

    /**
     * query、insert、update、delete condition
     */
    private Document condition;

    /**
     * column to select
     */
    private Document selectColumn;
}
