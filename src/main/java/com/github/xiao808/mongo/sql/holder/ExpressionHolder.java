package com.github.xiao808.mongo.sql.holder;

import com.alibaba.druid.sql.ast.SQLExpr;

/**
 * Only for input/output visitor purposes.
 */
public class ExpressionHolder {
    private SQLExpr expression;

    /**
     * Default constructor.
     * @param expression the expression
     */
    public ExpressionHolder(final SQLExpr expression) {
        this.expression = expression;
    }

    /**
     * get the expression.
     * @return the expression.
     */
    public SQLExpr getExpression() {
        return expression;
    }

    /**
     * set the expression.
     * @param expression the expression
     */
    public void setExpression(final SQLExpr expression) {
        this.expression = expression;
    }

}
