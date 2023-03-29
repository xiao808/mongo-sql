package com.github.xiao808.mongo.sql.utils;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.expr.SQLInListExpr;
import com.alibaba.druid.sql.ast.expr.SQLValuableExpr;
import com.github.xiao808.mongo.sql.ParseException;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Wrapper around mongo ObjectId.
 */
public class ObjectIdFunction {
    private final Object value;
    private final String column;
    private final SQLExpr comparisonExpression;

    /**
     * Default constructor.
     *
     * @param column     the column that is an objectId
     * @param value      the value of the objectId
     * @param expression {@link SQLExpr}
     */
    public ObjectIdFunction(final String column, final Object value, final SQLExpr expression) {
        this.column = column;
        this.value = value;
        this.comparisonExpression = expression;
    }

    /**
     * get the column that is an ObjectId.
     *
     * @return the column
     */
    public String getColumn() {
        return column;
    }

    /**
     * convert this ObjectId into a mongo document.
     *
     * @return the mongo document
     * @throws ParseException when the objectId could not be converted into a document.
     */
    public Object toDocument() throws ParseException {
        if (comparisonExpression instanceof SQLBinaryOpExpr) {
            SQLBinaryOpExpr expr = ((SQLBinaryOpExpr) comparisonExpression);
            SQLBinaryOperator operator = expr.getOperator();
            switch (operator) {
                case Equality:
                    return new ObjectId(value.toString());
                case NotEqual:
                    return new Document("$ne", new ObjectId(value.toString()));
                default:
                    throw new ParseException("could not convert ObjectId function into document");
            }
        } else if (comparisonExpression instanceof SQLInListExpr) {
            SQLInListExpr inExpression = (SQLInListExpr) comparisonExpression;
            List<SQLExpr> targetList = inExpression.getTargetList();
            return new Document(inExpression.isNot() ? "$nin" : "$in", targetList.stream().map(sqlExpr -> new ObjectId(((SQLValuableExpr) sqlExpr).getValue().toString())).collect(Collectors.toList()));
        }
        throw new ParseException("could not convert ObjectId function into document");
    }
}
