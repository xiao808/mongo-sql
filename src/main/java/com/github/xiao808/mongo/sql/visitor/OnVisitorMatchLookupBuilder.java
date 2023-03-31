package com.github.xiao808.mongo.sql.visitor;

import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.visitor.SQLASTVisitor;
import com.github.xiao808.mongo.sql.utils.SqlUtils;

/**
 * Generate lookup subpipeline match step from on clause. For optimization,
 * this must combine with where part of joined collection
 */
public class OnVisitorMatchLookupBuilder implements SQLASTVisitor {
    private String joinAliasTable;
    private String baseAliasTable;

    /**
     * Default constructor.
     *
     * @param joinAliasTable the alias for the join table.
     * @param baseAliasTable the alias for the base table.
     */
    public OnVisitorMatchLookupBuilder(final String joinAliasTable, final String baseAliasTable) {
        this.joinAliasTable = joinAliasTable;
        this.baseAliasTable = baseAliasTable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean visit(final SQLPropertyExpr column) {
        if (SqlUtils.isColumn(column)) {
            String columnName;
            if (column.getResolvedTableSource() != null) {
                columnName = SqlUtils.getColumnNameFromColumn(column);
            } else {
                columnName = column.getName();
            }
            if (!SqlUtils.isTableAliasOfColumn(column, joinAliasTable)) {
                if (column.getResolvedTableSource() == null || SqlUtils.isTableAliasOfColumn(column, baseAliasTable)) {
                    //we know let var don't have table inside
                    column.setName("$$" + columnName.replace(".", "_").toLowerCase());
                } else {
                    column.setName("$$" + column.getName().replace(".", "_").toLowerCase());
                }
                column.setResolvedTableSource(null);
            } else {
                column.setResolvedTableSource(null);
                column.setName("$" + columnName);
            }
        }
        return false;
    }
}
