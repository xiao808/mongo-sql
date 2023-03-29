package com.github.xiao808.mongo.sql.holder.from;

import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSubqueryTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.github.xiao808.mongo.sql.FieldType;
import com.github.xiao808.mongo.sql.ParseException;

import java.util.HashMap;
import java.util.Map;

/**
 * Class that holds information about the FROM section of the query.
 */
public class FromHolder {
    private FieldType defaultFieldType;
    private Map<String, FieldType> fieldNameToFieldTypeMapping;

    private SQLTableSource baseFrom;
    private String baseAlias;
    private Map<String, SQLTableSource> aliasToTable = new HashMap<>();
    private Map<SQLTableSource, String> tableToAlias = new HashMap<>();
    private Map<SQLTableSource, SQLInfoHolder> fromToSQLHolder = new HashMap<>();

    /**
     * Default Constructor.
     *
     * @param defaultFieldType            the default {@link FieldType}
     * @param fieldNameToFieldTypeMapping the mapping from field name to {@link FieldType}
     */
    public FromHolder(final FieldType defaultFieldType, final Map<String, FieldType> fieldNameToFieldTypeMapping) {
        this.defaultFieldType = defaultFieldType;
        this.fieldNameToFieldTypeMapping = fieldNameToFieldTypeMapping;
    }

    private void addToSQLHolderMap(final SQLTableSource from)
            throws ParseException {
        if (from instanceof SQLExprTableSource) {
            SQLExprTableSource table = (SQLExprTableSource) from;
            fromToSQLHolder.put(table, new SQLTableInfoHolder(table.getTableName()));
        } else if (from instanceof SQLSubqueryTableSource) {
            SQLSubqueryTableSource subselect = (SQLSubqueryTableSource) from;
            fromToSQLHolder.put(from, SQLCommandInfoHolder.Builder
                    .create(defaultFieldType, fieldNameToFieldTypeMapping)
                    .setPlainSelect(subselect.getSelect().getQueryBlock())
                    .build());
        } else {
            //Not happen SubJoin, not supported previously
            return;
        }
    }

    private void addBaseFrom(final SQLTableSource from)
            throws ParseException {
        baseFrom = from;
        addToSQLHolderMap(from);
    }

    private void addBaseFrom(final SQLTableSource from, final String alias)
            throws ParseException {
        addBaseFrom(from);
        if (alias != null) {
            baseAlias = alias;
            aliasToTable.put(alias, from);
        }
        tableToAlias.put(from, alias);
    }

    /**
     * Add information from the From clause of this query.
     *
     * @param from  the {@link SQLTableSource}
     * @param alias the alias
     * @throws ParseException if there is an issue processing
     *                        components from the sql statement
     * @throws ParseException if there is a problem processing the {@link SQLTableSource}
     */
    public void addFrom(final SQLTableSource from, final String alias)
            throws ParseException {
        if (baseFrom != null) {
            if (alias != null) {
                aliasToTable.put(alias, from);
            }
            tableToAlias.put(from, alias);
            addToSQLHolderMap(from);
        } else {
            addBaseFrom(from, alias);
        }
    }

    /**
     * Get the table name from the base from.
     *
     * @return the table name from the base from.
     */
    public String getBaseFromTableName() {
        return fromToSQLHolder.get(baseFrom).getBaseTableName();
    }

    /**
     * Get the base {@link SQLTableSource}.
     *
     * @return the base {@link SQLTableSource}
     */
    public SQLTableSource getBaseFrom() {
        return baseFrom;
    }

    /**
     * get the {@link SQLInfoHolder} from base {@link SQLTableSource}.
     *
     * @return the {@link SQLInfoHolder} from base {@link SQLTableSource}
     */
    public SQLInfoHolder getBaseSQLHolder() {
        return fromToSQLHolder.get(baseFrom);
    }

    /**
     * get the {@link SQLInfoHolder} from the provided {@link SQLTableSource}.
     *
     * @param fromItem the {@link SQLTableSource} from the sql query
     * @return the {@link SQLInfoHolder} from the provided {@link SQLTableSource}
     */
    public SQLInfoHolder getSQLHolder(final SQLTableSource fromItem) {
        return fromToSQLHolder.get(fromItem);
    }

    /**
     * get the base alias table.
     *
     * @return the base alias table
     */
    public String getBaseAliasTable() {
        return baseAlias;
    }

}
