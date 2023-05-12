package com.github.xiao808.mongo.sql;

import com.alibaba.druid.sql.ast.expr.SQLAggregateExpr;
import org.bson.Document;

import java.util.function.Function;

import static com.github.xiao808.mongo.sql.MongoIdConstants.DOLLAR;

/**
 * aggregate function enum
 *
 * @author zengxiao
 * @date 2023/5/11 11:34
 * @since 1.0
 **/
public enum AggregateEnum {

    /**
     * avg
     */
    AVG("avg", x -> {
        return new Document(LexerConstants.AVG, DOLLAR + x.getArguments().get(0).toString());
    }),
    /**
     * min
     */
    MIN("min", x -> {
        return new Document(LexerConstants.MIN, DOLLAR + x.getArguments().get(0).toString());
    }),
    /**
     * max
     */
    MAX("max", x -> {
        return new Document(LexerConstants.MAX, DOLLAR + x.getArguments().get(0).toString());
    }),
    /**
     * sum
     */
    SUM("sum", x -> {
        return new Document(LexerConstants.SUM, DOLLAR + x.getArguments().get(0).toString());
    }),
    /**
     * count
     */
    COUNT("count", x -> {
        return new Document(LexerConstants.SUM, 1);
    });

    /**
     * method name
     */
    private final String name;

    /**
     * mongo function operator for sql lexer
     */
    private final Function<SQLAggregateExpr, Document> mapper;

    AggregateEnum(String name, Function<SQLAggregateExpr, Document> mapper) {
        this.name = name;
        this.mapper = mapper;
    }

    public String getName() {
        return name;
    }

    public Function<SQLAggregateExpr, Document> getMapper() {
        return mapper;
    }
}
