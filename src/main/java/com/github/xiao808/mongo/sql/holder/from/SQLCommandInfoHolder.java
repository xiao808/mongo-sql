package com.github.xiao808.mongo.sql.holder.from;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLAllColumnExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectOrderByItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectQuery;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.ast.statement.SQLUnionQuery;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.parser.SQLParserUtils;
import com.alibaba.druid.util.DruidDataSourceUtils;
import com.github.xiao808.mongo.sql.FieldType;
import com.github.xiao808.mongo.sql.ParseException;
import com.github.xiao808.mongo.sql.SQLCommandType;
import com.github.xiao808.mongo.sql.holder.AliasHolder;
import com.github.xiao808.mongo.sql.utils.SqlUtils;
import com.github.xiao808.mongo.sql.visitor.ExpVisitorEraseAliasTableBaseBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of {@link SQLInfoHolder} to hold information about the sql query and it's structure.
 */
public final class SQLCommandInfoHolder implements SQLInfoHolder {
    private final SQLCommandType sqlCommandType;
    private final boolean isDistinct;
    private final boolean isCountAll;
    private final boolean isTotalGroup;
    private final FromHolder from;
    private final long limit;
    private final long offset;
    private final SQLExpr whereClause;
    private final List<SQLSelectItem> selectItems;
    private final List<SQLTableSource> joins;
    private final List<String> groupBys;
    private final List<SQLSelectOrderByItem> orderByElements;
    private final AliasHolder aliasHolder;
    private final SQLExpr havingClause;
    private final List<SQLUpdateSetItem> updateSets;


    private SQLCommandInfoHolder(final Builder builder) {
        this.sqlCommandType = builder.sqlCommandType;
        this.whereClause = builder.whereClause;
        this.isDistinct = builder.isDistinct;
        this.isCountAll = builder.isCountAll;
        this.isTotalGroup = builder.isTotalGroup;
        this.from = builder.from;
        this.limit = builder.limit;
        this.offset = builder.offset;
        this.selectItems = builder.selectItems;
        this.joins = builder.joins;
        this.groupBys = builder.groupBys;
        this.havingClause = builder.havingClause;
        this.orderByElements = builder.orderByElements;
        this.aliasHolder = builder.aliasHolder;
        this.updateSets = builder.updateSets;
    }

    @Override
    public String getBaseTableName() {
        return from.getBaseFromTableName();
    }

    /**
     * get if distinct was used in the sql query.
     * @return true if distinct
     */
    public boolean isDistinct() {
        return isDistinct;
    }

    /**
     * true if count(*) is used.
     * @return true if count(*) is used
     */
    public boolean isCountAll() {
        return isCountAll;
    }

    /**
     * Will return true if any of the {@link SQLSelectItem}s has a function that justifies aggregation like max().
     * @return true if any of the {@link SQLSelectItem}s has a function that justifies aggregation like max()
     */
    public boolean isTotalGroup() {
        return isTotalGroup;
    }

    /**
     * get the base table name from this query.
     * @return the base table name
     */
    public String getTable() {
        return from.getBaseFromTableName();
    }

    /**
     * get the {@link FromHolder} that holds information about the from information in the query.
     * @return the {@link FromHolder}
     */
    public FromHolder getFromHolder() {
        return this.from;
    }

    /**
     * get the limit used in the sql query.
     * @return the limit
     */
    public long getLimit() {
        return limit;
    }

    /**
     * get the offset from the sql query.
     * @return the offset from the sql query
     */
    public long getOffset() {
        return offset;
    }

    /**
     * get the where clause from the query.
     * @return the where clause
     */
    public SQLExpr getWhereClause() {
        return whereClause;
    }

    /**
     * get the select items from the query.
     * @return the select items from the query
     */
    public List<SQLSelectItem> getSelectItems() {
        return selectItems;
    }

    /**
     * get the joins from the query.
     * @return the joins from the query
     */
    public List<SQLTableSource> getJoins() {
        return joins;
    }

    /**
     * get the groupbys from the query.
     * @return the groupbys
     */
    public List<String> getGroupBys() {
        return groupBys;
    }

    /**
     * get the having clause from the sql query.
     * @return the having clause from the sql query
     */
    public SQLExpr getHavingClause() {
        return havingClause;
    }

    /**
     * get the order by elements from the query.
     * @return the order by elements
     */
    public List<SQLSelectOrderByItem> getOrderByElements() {
        return orderByElements;
    }

    /**
     * Get the {@link SQLCommandType} for this query.
     * @return the {@link SQLCommandType}
     */
    public SQLCommandType getSqlCommandType() {
        return sqlCommandType;
    }

    /**
     * Get the {@link AliasHolder} for this query.
     * @return the alias holder
     */
    public AliasHolder getAliasHolder() {
        return aliasHolder;
    }

    /**
     * get the update sets used for updates.
     * @return the update sets
     */
    public List<SQLUpdateSetItem> getUpdateSets() {
        return updateSets;
    }

    /**
     * Builder for {@link SQLCommandInfoHolder}.
     */
    public static final class Builder {
        private final FieldType defaultFieldType;
        private final Map<String, FieldType> fieldNameToFieldTypeMapping;
        private SQLCommandType sqlCommandType;
        private SQLExpr whereClause;
        private List<SQLUpdateSetItem> updateSets = new ArrayList<>();
        private boolean isDistinct = false;
        private boolean isCountAll = false;
        private boolean isTotalGroup = false;
        private FromHolder from;
        private long limit = -1;
        private long offset = -1;
        private List<SQLSelectItem> selectItems = new ArrayList<>();
        private List<SQLTableSource> joins = new ArrayList<>();
        private List<String> groupBys = new ArrayList<>();
        private SQLExpr havingClause;
        private List<SQLSelectOrderByItem> orderByElements = new ArrayList<>();
        private AliasHolder aliasHolder;

        private Builder(final FieldType defaultFieldType, final Map<String, FieldType> fieldNameToFieldTypeMapping) {
            this.defaultFieldType = defaultFieldType;
            this.fieldNameToFieldTypeMapping = fieldNameToFieldTypeMapping;
        }

        private FromHolder generateFromHolder(final FromHolder tholder,
                                              final SQLTableSource fromItem, final List<SQLTableSource> ljoin)
                throws ParseException {

            FromHolder returnValue = tholder;
            String alias = fromItem.getAlias();
            returnValue.addFrom(fromItem, alias);

            if (ljoin != null) {
                for (SQLTableSource j : ljoin) {
                    SQLJoinTableSource tableSource = (SQLJoinTableSource) j;
                    if (tableSource.getJoinType() == SQLJoinTableSource.JoinType.INNER_JOIN || tableSource.getJoinType() == SQLJoinTableSource.JoinType.LEFT_OUTER_JOIN) {
                        returnValue = generateFromHolder(returnValue, tableSource.getRight(), null);
                    } else {
                        throw new ParseException("Join type not supported");
                    }
                }
            }
            return returnValue;
        }

        /**
         * Set the select or delete statement from the parsed sql string.
         * @param statement the {@link com.alibaba.druid.sql.ast.SQLStatement}
         * @return the builder
         * @throws ParseException if there is an issue
         * the parsing the sql
         * @throws ParseException if there is an issue the parsing the sql
         */
        public Builder setStatement(final SQLStatement statement)
                throws ParseException {

            if (statement instanceof SQLSelectStatement) {
                sqlCommandType = SQLCommandType.SELECT;
                SQLSelect selectBody = ((SQLSelectStatement) statement).getSelect();
                SQLSelectQuery query = selectBody.getQuery();
                if (query instanceof SQLUnionQuery) {
                    // union is not supported now.
                    SQLUnionQuery setOperationList = (SQLUnionQuery) query;
                    if (setOperationList.getRelations() != null
                            && setOperationList.getRelations().size() == 1
                            && setOperationList.getRelations().get(0) instanceof SQLSelectQueryBlock) {
                        return setPlainSelect((SQLSelectQueryBlock) setOperationList.getRelations().get(0));
                    }
                } else if (query instanceof SQLSelectQueryBlock) {
                    // parse select query block
                    return setPlainSelect((SQLSelectQueryBlock) query);
                }

                throw new ParseException("No supported sentence");
            } else if (statement instanceof SQLDeleteStatement) {
                sqlCommandType = SQLCommandType.DELETE;
                return setDelete((SQLDeleteStatement) statement);
            } else if (statement instanceof SQLUpdateStatement) {
                sqlCommandType = SQLCommandType.UPDATE;
                return setUpdate((SQLUpdateStatement) statement);
            } else {
                throw new ParseException("No supported sentence");
            }
        }

        /**
         * Set the update information for this query if it is a update query.
         * @param update the {@link SQLUpdateStatement} object
         * @return the builder
         * @throws ParseException if there is an issue
         * parsing the sql
         * @throws ParseException if there is an issue parsing the sql
         */
        public Builder setUpdate(final SQLUpdateStatement update)
                throws ParseException {
            SqlUtils.isTrue(update.getFrom() != null,
                    "there must be a table specified for update");
            from = generateFromHolder(new FromHolder(this.defaultFieldType,
                    this.fieldNameToFieldTypeMapping), update.getFrom(), null);
            whereClause = update.getWhere();
            updateSets.addAll(update.getItems());
            return this;
        }

        /**
         * Set the select query information for this query.
         * @param plainSelect the {@link SQLSelect}
         * @return the builder
         * @throws ParseException if there is an issue
         * the parsing the sql
         * @throws ParseException if there is an issue the parsing the sql
         */
        public Builder setPlainSelect(final SQLSelectQueryBlock plainSelect)
                throws ParseException {
            SqlUtils.isTrue(plainSelect != null,
                    "could not parseNaturalLanguageDate SELECT statement from query");
            SQLTableSource from = plainSelect.getFrom();
            SqlUtils.isTrue(from != null,
                    "could not find table to query.  Only one simple table name is supported.");
            whereClause = plainSelect.getWhere();
            isDistinct = plainSelect.isDistinct();
            isCountAll = SqlUtils.isCountAll(plainSelect.getSelectList());

            List<SQLTableSource> joinTables = new ArrayList<>();
            if (from instanceof SQLJoinTableSource) {
                joinTables.add(from);
                from = ((SQLJoinTableSource)from).getLeft();
            }
            this.from = generateFromHolder(new FromHolder(this.defaultFieldType,
                    this.fieldNameToFieldTypeMapping), from, joinTables);
            limit = SqlUtils.getLimitAsLong(plainSelect.getLimit());
            offset = SqlUtils.getOffsetAsLong(plainSelect.getLimit());
            Optional.ofNullable(plainSelect.getOrderBy()).ifPresent(sqlOrderBy -> {
                orderByElements = sqlOrderBy.getItems();
            });
            selectItems = plainSelect.getSelectList();
            joins = joinTables;
            groupBys = SqlUtils.getGroupByColumnReferences(plainSelect);
            Optional.ofNullable(plainSelect.getGroupBy()).ifPresent(sqlSelectGroupByClause -> {
                havingClause = sqlSelectGroupByClause.getHaving();
            });
            aliasHolder = generateHashAliasFromSelectItems(selectItems);
            isTotalGroup = SqlUtils.isTotalGroup(selectItems);
            SqlUtils.isTrue(from != null,
                    "could not find table to query.  Only one simple table name is supported.");
            return this;
        }

        /**
         * Set the delete information for this query if it is a delete query.
         * @param delete the {@link SQLDeleteStatement} object
         * @return the builder
         * @throws ParseException if there is an issue
         * parsing the sql
         * @throws ParseException if there is an issue parsing the sql
         */
        public Builder setDelete(final SQLDeleteStatement delete) throws ParseException {
            SqlUtils.isTrue(delete.getFrom() == null, "there should only be on table specified for deletes");
            from = generateFromHolder(new FromHolder(this.defaultFieldType,
                    this.fieldNameToFieldTypeMapping), delete.getFrom(), null);
            whereClause = delete.getWhere();
            return this;
        }

        private AliasHolder generateHashAliasFromSelectItems(final List<SQLSelectItem> selectItems) {
            HashMap<String, String> aliasFromFieldHash = new HashMap<>();
            HashMap<String, String> fieldFromAliasHash = new HashMap<>();
            for (SQLSelectItem sitem : selectItems) {
                if (!(sitem.getExpr() instanceof SQLAllColumnExpr)) {
                    String aliasStr = sitem.getAlias();
                    SQLExpr seitem = sitem.getExpr();
                    if (aliasStr != null) {
                        seitem.accept(new ExpVisitorEraseAliasTableBaseBuilder(
                                this.from.getBaseAliasTable()));
                        String expStr = seitem.toString();
                        aliasFromFieldHash.put(expStr, aliasStr);
                        fieldFromAliasHash.put(aliasStr, expStr);
                    }
                }
            }
            return new AliasHolder(aliasFromFieldHash, fieldFromAliasHash);
        }

        /**
         * Build a {@link SQLCommandInfoHolder}.
         * @return the {@link SQLCommandInfoHolder}
         */
        public SQLCommandInfoHolder build() {
            return new SQLCommandInfoHolder(this);
        }

        /**
         * create a {@link Builder}.
         * @param defaultFieldType the default {@link FieldType}
         * @param fieldNameToFieldTypeMapping the field name to {@link FieldType} map
         * @return the builder
         */
        public static Builder create(final FieldType defaultFieldType,
                                     final Map<String, FieldType> fieldNameToFieldTypeMapping) {
            return new Builder(defaultFieldType, fieldNameToFieldTypeMapping);
        }
    }
}
