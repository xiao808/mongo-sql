package com.github.xiao808.mongo.sql.visitor;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.expr.SQLInListExpr;
import com.alibaba.druid.sql.ast.expr.SQLNullExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.visitor.SQLASTVisitor;
import com.github.xiao808.mongo.sql.holder.ExpressionHolder;
import com.github.xiao808.mongo.sql.utils.SqlUtils;
import org.apache.commons.lang.mutable.MutableBoolean;

/**
 * Generate lookup match from where. For optimization, this must combine with "on" part of joined collection.
 */
public class WhereVisitorMatchAndLookupPipelineMatchBuilder implements SQLASTVisitor {
    private String baseAliasTable;
    /**
     * This expression will have the where part of baseAliasTable.
     */
    private ExpressionHolder outputMatch = null;
    /**
     * This flag will be true is there is some "or" expression.
     * It that case match expression go in the main pipeline after lookup.
     */
    private final MutableBoolean haveOrExpression;
    private boolean isBaseAliasOrValue;

    /**
     * Default constructor.
     *
     * @param baseAliasTable   the alias for the base table.
     * @param outputMatch      the {@link ExpressionHolder}
     * @param haveOrExpression if there is an or expression
     */
    public WhereVisitorMatchAndLookupPipelineMatchBuilder(final String baseAliasTable,
                                                          final ExpressionHolder outputMatch,
                                                          final MutableBoolean haveOrExpression) {
        this.baseAliasTable = baseAliasTable;
        this.outputMatch = outputMatch;
        this.haveOrExpression = haveOrExpression;
    }

    private ExpressionHolder setOrAndExpression(final ExpressionHolder baseExp, final SQLExpr newExp) {
        SQLExpr exp;
        if (baseExp.getExpression() != null) {
            exp = new SQLBinaryOpExpr(baseExp.getExpression(), SQLBinaryOperator.BooleanAnd, newExp);
        } else {
            exp = newExp;
        }
        baseExp.setExpression(exp);
        return baseExp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean visit(final SQLPropertyExpr column) {
        if (SqlUtils.isColumn(column)) {
            this.isBaseAliasOrValue = SqlUtils.isTableAliasOfColumn(column, this.baseAliasTable);
        }
        return false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean visit(final SQLBinaryOpExpr expr) {
        SQLBinaryOperator operator = expr.getOperator();
        // and
        if (operator == SQLBinaryOperator.BooleanOr) {
            this.haveOrExpression.setValue(true);
        } else if (operator == SQLBinaryOperator.Is && expr.getRight() instanceof SQLNullExpr) {
            if (this.isBaseAliasOrValue) {
                this.setOrAndExpression(outputMatch, expr);
            }
        } else if (operator == SQLBinaryOperator.IsNot && expr.getRight() instanceof SQLNullExpr) {
            if (this.isBaseAliasOrValue) {
                this.setOrAndExpression(outputMatch, expr);
            }
        } else {
            this.isBaseAliasOrValue = true;
            expr.getLeft().accept(this);
            if (!this.isBaseAliasOrValue) {
                expr.getRight().accept(this);
            } else {
                expr.getRight().accept(this);
                if (this.isBaseAliasOrValue && operator != SQLBinaryOperator.BooleanAnd) {
                    this.setOrAndExpression(outputMatch, expr);
                }
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean visit(final SQLInListExpr expr) {
        this.isBaseAliasOrValue = true;
        expr.getExpr().accept(this);
        if (!this.isBaseAliasOrValue) {
            expr.getTargetList().forEach(sqlExpr -> sqlExpr.accept(this));
        } else {
            expr.getTargetList().forEach(sqlExpr -> sqlExpr.accept(this));
            if (this.isBaseAliasOrValue) {
                this.setOrAndExpression(outputMatch, expr);
            }
        }
        return false;
    }
}
