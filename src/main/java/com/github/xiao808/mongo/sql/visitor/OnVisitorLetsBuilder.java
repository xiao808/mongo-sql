package com.github.xiao808.mongo.sql.visitor;

import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.visitor.SQLASTVisitor;
import com.github.xiao808.mongo.sql.utils.SqlUtils;
import org.bson.Document;

/**
 * Generate lookup lets from on clause. All fields without table fields.
 */
public class OnVisitorLetsBuilder implements SQLASTVisitor {
    private Document onDocument;
    private String joinAliasTable;
    private String baseAliasTable;

    /**
     * Default constructor.
     *
     * @param onDocument     the new document.
     * @param joinAliasTable the alias for the join table
     * @param baseAliasTable the alias for the base table
     */
    public OnVisitorLetsBuilder(final Document onDocument, final String joinAliasTable, final String baseAliasTable) {
        this.onDocument = onDocument;
        this.joinAliasTable = joinAliasTable;
        this.baseAliasTable = baseAliasTable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean visit(final SQLPropertyExpr column) {
        if (!SqlUtils.isTableAliasOfColumn(column, joinAliasTable)) {
            String columnName;
            if (SqlUtils.isTableAliasOfColumn(column, baseAliasTable)) {
                columnName = SqlUtils.getColumnNameFromColumn(column);
            } else {
                columnName = column.getName();
            }
            onDocument.put(columnName.replace(".", "_").toLowerCase(), "$" + columnName);
        }
        return false;
    }
}
