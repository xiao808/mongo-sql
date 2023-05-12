package com.github.xiao808.mongo.sql;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;
import org.bson.Document;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.github.xiao808.mongo.sql.MongoIdConstants.DOLLAR;

/**
 * method function enum
 *
 * @author zengxiao
 * @date 2023/5/11 10:40
 * @since 1.0
 **/
public enum FunctionEnum {

    /**
     * trim
     */
    TRIM("trim", x -> {
        List<SQLExpr> arguments = x.getArguments();
        SQLExpr calculateField = arguments.get(0);
        return new Document(LexerConstants.TRIM, DOLLAR + calculateField.toString());
    }),
    /**
     * concat
     */
    CONCAT("concat", x -> {
        List<SQLExpr> arguments = x.getArguments();
        return new Document(LexerConstants.CONCAT, arguments.stream().map(sqlExpr -> {
            if (sqlExpr instanceof SQLName) {
                return DOLLAR + sqlExpr;
            } else {
                return sqlExpr.toString();
            }
        }).collect(Collectors.toList()));
    }),
    /**
     * abs
     */
    ABS("abs", x -> {
        List<SQLExpr> arguments = x.getArguments();
        SQLExpr calculateField = arguments.get(0);
        return new Document(LexerConstants.ABS, DOLLAR + calculateField.toString());
    }),
    /**
     * add
     */
    ADD("add", x -> {
        List<SQLExpr> arguments = x.getArguments();
        return new Document(LexerConstants.ADD, arguments.stream().map(sqlExpr -> {
            if (sqlExpr instanceof SQLName) {
                return DOLLAR + sqlExpr;
            } else {
                return sqlExpr.toString();
            }
        }).collect(Collectors.toList()));
    }),
    /**
     * ceil
     */
    CEIL("ceil", x -> {
        List<SQLExpr> arguments = x.getArguments();
        return new Document(LexerConstants.CEIL, arguments.stream().map(sqlExpr -> {
            if (sqlExpr instanceof SQLName) {
                return DOLLAR + sqlExpr;
            } else {
                return sqlExpr.toString();
            }
        }).collect(Collectors.toList()));
    }),
    /**
     * divide
     */
    DIVIDE("divide", x -> {
        List<SQLExpr> arguments = x.getArguments();
        return new Document(LexerConstants.DIVIDE, arguments.stream().map(sqlExpr -> {
            if (sqlExpr instanceof SQLName) {
                return DOLLAR + sqlExpr;
            } else {
                return sqlExpr.toString();
            }
        }).collect(Collectors.toList()));
    }),
    /**
     * mod
     */
    MOD("mod", x -> {
        List<SQLExpr> arguments = x.getArguments();
        return new Document(LexerConstants.MOD, arguments.stream().map(sqlExpr -> {
            if (sqlExpr instanceof SQLName) {
                return DOLLAR + sqlExpr;
            } else {
                return sqlExpr.toString();
            }
        }).collect(Collectors.toList()));
    }),
    /**
     * pow
     */
    POW("pow", x -> {
        List<SQLExpr> arguments = x.getArguments();
        return new Document(LexerConstants.POW, arguments.stream().map(sqlExpr -> {
            if (sqlExpr instanceof SQLName) {
                return DOLLAR + sqlExpr;
            } else {
                return sqlExpr.toString();
            }
        }).collect(Collectors.toList()));
    }),
    /**
     * convert date to string
     */
    DATE_TO_STRING("dateToString", x -> {
        List<SQLExpr> arguments = x.getArguments();
        return new Document(LexerConstants.DATE_TO_STRING, new Document(Map.of(
                LexerConstants.DATE, DOLLAR + arguments.get(0).toString(),
                LexerConstants.FORMAT, arguments.get(1).toString()
        )));
    }),
    /**
     * year
     */
    YEAR("year", x -> {
        List<SQLExpr> arguments = x.getArguments();
        SQLExpr calculateField = arguments.get(0);
        return new Document(LexerConstants.YEAR, DOLLAR + calculateField.toString());
    }),
    /**
     * month
     */
    MONTH("month", x -> {
        List<SQLExpr> arguments = x.getArguments();
        SQLExpr calculateField = arguments.get(0);
        return new Document(LexerConstants.MONTH, DOLLAR + calculateField.toString());
    }),
    /**
     * day of month
     */
    DAY_OF_MONTH("dayOfMonth", x -> {
        List<SQLExpr> arguments = x.getArguments();
        SQLExpr calculateField = arguments.get(0);
        return new Document(LexerConstants.DAY_OF_MONTH, DOLLAR + calculateField.toString());
    }),
    /**
     * hour
     */
    HOUR("hour", x -> {
        List<SQLExpr> arguments = x.getArguments();
        SQLExpr calculateField = arguments.get(0);
        return new Document(LexerConstants.HOUR, DOLLAR + calculateField.toString());
    }),
    /**
     * minute
     */
    MINUTE("minute", x -> {
        List<SQLExpr> arguments = x.getArguments();
        SQLExpr calculateField = arguments.get(0);
        return new Document(LexerConstants.MINUTE, DOLLAR + calculateField.toString());
    }),
    /**
     * second
     */
    SECOND("second", x -> {
        List<SQLExpr> arguments = x.getArguments();
        SQLExpr calculateField = arguments.get(0);
        return new Document(LexerConstants.SECOND, DOLLAR + calculateField.toString());
    });

    /**
     * method name
     */
    private final String name;

    /**
     * mongo function operator for sql lexer
     */
    private final Function<SQLMethodInvokeExpr, Document> mapper;

    FunctionEnum(String name, Function<SQLMethodInvokeExpr, Document> mapper) {
        this.name = name;
        this.mapper = mapper;
    }

    public String getName() {
        return name;
    }

    public Function<SQLMethodInvokeExpr, Document> getMapper() {
        return mapper;
    }
}
