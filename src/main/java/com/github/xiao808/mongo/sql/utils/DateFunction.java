package com.github.xiao808.mongo.sql.utils;

import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.github.xiao808.mongo.sql.ParseException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Object that serves as a wrapper around a date.
 */
public class DateFunction {
    private final Date date;
    private final String column;
    private String comparisonExpression = "$eq";

    /**
     * Default Constructor.
     *
     * @param format             the format in {@link java.text.SimpleDateFormat} on "natural"
     * @param value              the date to format
     * @param column             the column that is a date
     * @param comparisonOperator the {@link SQLBinaryOpExpr}
     * @throws ParseException if the date can not be parsed
     */
    public DateFunction(final String format, final String value,
                        final String column, final SQLBinaryOpExpr comparisonOperator) throws ParseException {
        if ("natural".equals(format)) {
            this.date = SqlUtils.parseNaturalLanguageDate(value);
        } else {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault());
            this.date = Date.from(Instant.from(dateTimeFormatter.parse(value)));
        }
        this.column = column;
        setComparisonFunction(comparisonOperator);
    }

    /**
     * get the date object.
     *
     * @return the date
     */
    public Date getDate() {
        return new Date(date.getTime());
    }

    /**
     * get the column that is a date.
     *
     * @return the column
     */
    public String getColumn() {
        return column;
    }

    private void setComparisonFunction(final SQLBinaryOpExpr comparisonFunction) throws ParseException {
        SQLBinaryOperator operator = comparisonFunction.getOperator();
        switch (operator) {
            case GreaterThanOrEqual:
                this.comparisonExpression = "$gte";
                break;
            case GreaterThan:
                this.comparisonExpression = "$gt";
                break;
            case LessThanOrEqual:
                this.comparisonExpression = "$lte";
                break;
            case LessThan:
                this.comparisonExpression = "$lt";
                break;
            case Equality:
                this.comparisonExpression = "$eq";
                break;
            default:
                throw new ParseException("could not parseNaturalLanguageDate string expression: " + comparisonFunction);
        }
    }

    /**
     * get comparison expression for this Date function.
     *
     * @return the comparison expression
     */
    public String getComparisonExpression() {
        return comparisonExpression;
    }
}
