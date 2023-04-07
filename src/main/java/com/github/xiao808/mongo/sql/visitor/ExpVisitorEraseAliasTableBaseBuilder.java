package com.github.xiao808.mongo.sql.visitor;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLAllColumnExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.visitor.SQLASTVisitor;
import com.github.xiao808.mongo.sql.utils.SqlUtils;

/**
 * Generate lookup lets from on clause. All fields without table fields.
 */
public class ExpVisitorEraseAliasTableBaseBuilder implements SQLASTVisitor {
    private String baseAliasTable;

    /**
     * Default constructor.
     *
     * @param baseAliasTable the alias for the base table
     */
    public ExpVisitorEraseAliasTableBaseBuilder(final String baseAliasTable) {
        this.baseAliasTable = baseAliasTable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean visit(final SQLPropertyExpr column) {
        column.setName(column.getName().replaceFirst("[`\"]", "").replaceAll("[`\"]$", ""));
        SqlUtils.removeAliasFromColumn(column, baseAliasTable);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean visit(final SQLSelectItem selectExpressionItem) {
        SQLExpr expr = selectExpressionItem.getExpr();
        if (expr instanceof SQLPropertyExpr) {
            SQLPropertyExpr sqlPropertyExpr = (SQLPropertyExpr) expr;
            sqlPropertyExpr.setName(sqlPropertyExpr.getName().replaceFirst("[`\"]", "").replaceAll("[`\"]$", ""));
        }
        if (expr instanceof SQLIdentifierExpr) {
            SQLIdentifierExpr sqlPropertyExpr = (SQLIdentifierExpr) expr;
            sqlPropertyExpr.setName(sqlPropertyExpr.getName().replaceFirst("[`\"]", "").replaceAll("[`\"]$", ""));
        }
        SqlUtils.removeAliasFromSelectExpressionItem(selectExpressionItem, baseAliasTable);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean visit(final SQLAllColumnExpr allColumns) {
        //noop.... needed to avoid StackOverflowException
        return false;
    }

}
