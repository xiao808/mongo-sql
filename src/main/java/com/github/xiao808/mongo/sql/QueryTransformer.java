package com.github.xiao808.mongo.sql;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.SQLOrderingSpecification;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLAllColumnExpr;
import com.alibaba.druid.sql.ast.expr.SQLAllExpr;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;
import com.alibaba.druid.sql.ast.expr.SQLNullExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.expr.SQLValuesExpr;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectOrderByItem;
import com.alibaba.druid.sql.ast.statement.SQLSubqueryTableSource;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.github.xiao808.mongo.sql.holder.AliasHolder;
import com.github.xiao808.mongo.sql.holder.ExpressionHolder;
import com.github.xiao808.mongo.sql.holder.from.FromHolder;
import com.github.xiao808.mongo.sql.holder.from.SQLCommandInfoHolder;
import com.github.xiao808.mongo.sql.processor.HavingClauseProcessor;
import com.github.xiao808.mongo.sql.processor.JoinProcessor;
import com.github.xiao808.mongo.sql.processor.WhereClauseProcessor;
import com.github.xiao808.mongo.sql.utils.SqlUtils;
import com.github.xiao808.mongo.sql.visitor.ExpVisitorEraseAliasTableBaseBuilder;
import com.github.xiao808.mongo.sql.visitor.WhereVisitorMatchAndLookupPipelineMatchBuilder;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.apache.commons.lang.Validate.notNull;

/**
 * Main class responsible for query conversion.
 */
public final class QueryTransformer {
    private final SQLStatement sqlStatement;
    private final Integer aggregationBatchSize;
    private final Boolean aggregationAllowDiskUse;
    private final MongoDBQueryHolder mongoQueryHolder;

    private final Map<String, FieldType> fieldNameToFieldTypeMapping;
    private final FieldType defaultFieldType;
    private final SQLCommandInfoHolder sqlCommandInfoHolder;

    /**
     * Create a QueryConverter with an InputStream.
     *
     * @param sql                         sql
     * @param fieldNameToFieldTypeMapping mapping for each field
     * @param defaultFieldType            the default {@link FieldType} to be used
     * @param aggregationAllowDiskUse     set whether disk use is allowed during aggregation
     * @param aggregationBatchSize        set the batch size for aggregation
     * @throws ParseException when the sql query cannot be parsed
     */
    private QueryTransformer(final String sql, final DbType dbType, final Map<String, FieldType> fieldNameToFieldTypeMapping,
                             final FieldType defaultFieldType, final Boolean aggregationAllowDiskUse,
                             final Integer aggregationBatchSize) throws ParseException {
        this.aggregationAllowDiskUse = aggregationAllowDiskUse;
        this.aggregationBatchSize = aggregationBatchSize;
        this.sqlStatement = SQLUtils.parseSingleStatement(sql, dbType);
        this.defaultFieldType = defaultFieldType != null ? defaultFieldType : FieldType.UNKNOWN;
        this.sqlCommandInfoHolder = SQLCommandInfoHolder.Builder
                .create(defaultFieldType, fieldNameToFieldTypeMapping)
                .initSqlStatement(sqlStatement)
                .build();
        this.fieldNameToFieldTypeMapping = fieldNameToFieldTypeMapping != null
                ? fieldNameToFieldTypeMapping : Collections.emptyMap();
        this.mongoQueryHolder = getMongoQueryInternal(sqlCommandInfoHolder);
        validate();
    }

    private void validate() throws ParseException {
        List<SQLSelectItem> selectItems = sqlCommandInfoHolder.getSelectItems();
        List<SQLSelectItem> filteredItems = selectItems.stream().filter(sqlSelectItem -> !(sqlSelectItem.getExpr() instanceof SQLAllColumnExpr) && !(sqlSelectItem.getExpr() instanceof SQLAllExpr)).collect(Collectors.toList());
        SqlUtils.isFalse((selectItems.size() > 1
                || SqlUtils.isSelectAll(selectItems))
                && sqlCommandInfoHolder.isDistinct(), "cannot run distinct one more than one column");
        SqlUtils.isFalse(sqlCommandInfoHolder.getGroupBys().size() == 0
                        && selectItems.size() != filteredItems.size() && !SqlUtils.isSelectAll(selectItems)
                        && !SqlUtils.isCountAll(selectItems)
                        && !sqlCommandInfoHolder.isTotalGroup(),
                "illegal expression(s) found in select clause.  Only column names supported");
    }

    /**
     * get the object that you need to submit a query.
     *
     * @return the {@link MongoDBQueryHolder}
     * that contains all that is needed to describe the query to be run.
     */
    public MongoDBQueryHolder getDBMongoQueryHolder() {
        return mongoQueryHolder;
    }

    /**
     * get sql command info
     *
     * @return sql command info
     */
    public SQLCommandInfoHolder getSqlCommandInfoHolder() {
        return sqlCommandInfoHolder;
    }

    public SQLStatement getSqlStatement() {
        return sqlStatement;
    }

    /**
     * get aggregation batch size
     *
     * @return aggregation batch size
     */
    public Integer getAggregationBatchSize() {
        return aggregationBatchSize;
    }

    /**
     * whether using disk for aggregation
     *
     * @return whether using disk for aggregation
     */
    public Boolean getAggregationAllowDiskUse() {
        return aggregationAllowDiskUse;
    }

    /**
     * Will convert the query into aggregation steps.
     *
     * @param sqlCommandInfoHolder the {@link SQLCommandInfoHolder}
     * @return the aggregation steps
     * @throws ParseException if there is an issue parsing the sql
     */
    public List<Document> fromSQLCommandInfoHolderToAggregateSteps(final SQLCommandInfoHolder sqlCommandInfoHolder)
            throws ParseException {
        MongoDBQueryHolder queryHolder = getMongoQueryInternal(sqlCommandInfoHolder);
        return generateAggSteps(queryHolder, sqlCommandInfoHolder);
    }

    private MongoDBQueryHolder getMongoQueryInternal(final SQLCommandInfoHolder sqlCommandInfoHolder)
            throws ParseException {
        MongoDBQueryHolder queryHolder = new MongoDBQueryHolder(sqlCommandInfoHolder.getBaseTableName());
        Document document = new Document();
        //From Subquery
        if (sqlCommandInfoHolder.getFromHolder().getBaseFrom().getClass() == SQLSubqueryTableSource.class) {
            queryHolder.setPrevSteps(fromSQLCommandInfoHolderToAggregateSteps(
                    (SQLCommandInfoHolder) sqlCommandInfoHolder.getFromHolder().getBaseSQLHolder()));
            queryHolder.setRequiresMultistepAggregation(true);
        }

        if (sqlCommandInfoHolder.isDistinct()) {
            document.put(sqlCommandInfoHolder.getSelectItems().get(0).toString(), 1);
            queryHolder.setProjection(document);
            queryHolder.setDistinct(true);
        } else if (sqlCommandInfoHolder.getGroupBys().size() > 0) {
            List<String> groupBys = preprocessGroupBy(sqlCommandInfoHolder.getGroupBys(),
                    sqlCommandInfoHolder.getFromHolder());
            List<SQLSelectItem> selects = preprocessSelect(sqlCommandInfoHolder.getSelectItems(),
                    sqlCommandInfoHolder.getFromHolder());
            if (sqlCommandInfoHolder.getGroupBys().size() > 0) {
                queryHolder.setGroupBys(groupBys);
            }
            queryHolder.setProjection(createProjectionsFromSelectItems(selects, groupBys));
            AliasProjectionForGroupItems aliasProjectionForGroupItems = createAliasProjectionForGroupItems(
                    selects, groupBys);
            queryHolder.setAliasProjection(aliasProjectionForGroupItems.getDocument());
            queryHolder.setRequiresMultistepAggregation(true);
        } else if (sqlCommandInfoHolder.isTotalGroup()) {
            List<SQLSelectItem> selects = preprocessSelect(sqlCommandInfoHolder.getSelectItems(),
                    sqlCommandInfoHolder.getFromHolder());
            Document d = createProjectionsFromSelectItems(selects, null);
            queryHolder.setProjection(d);
            AliasProjectionForGroupItems aliasProjectionForGroupItems = createAliasProjectionForGroupItems(
                    selects, null);
            sqlCommandInfoHolder.getAliasHolder().combine(aliasProjectionForGroupItems.getFieldToAliasMapping());
            queryHolder.setAliasProjection(aliasProjectionForGroupItems.getDocument());
        } else if (!SqlUtils.isSelectAll(sqlCommandInfoHolder.getSelectItems())) {
            document.put("_id", 0);
            for (SQLSelectItem selectItem : sqlCommandInfoHolder.getSelectItems()) {
                SQLExpr selectExpressionItem = selectItem.getExpr();
                if (selectExpressionItem instanceof SQLPropertyExpr) {
                    SQLPropertyExpr c = (SQLPropertyExpr) selectExpressionItem;
                    //If we found alias of base table we ignore it because base table doesn't need alias, it's itself
                    String columnName = SqlUtils.removeAliasFromColumn(c, sqlCommandInfoHolder
                            .getFromHolder().getBaseAliasTable()).getSimpleName();
                    String alias = selectItem.getAlias();
                    document.put((alias != null ? alias : columnName), (alias != null ? "$" + columnName : 1));
                } else if (selectExpressionItem instanceof SQLSubqueryTableSource) {
                    throw new ParseException("Unsupported subselect expression");
                } else if (selectExpressionItem instanceof SQLMethodInvokeExpr) {
                    SQLMethodInvokeExpr f = (SQLMethodInvokeExpr) selectExpressionItem;
                    String columnName = f.toString();
                    String alias = selectItem.getAlias();
                    String key = (alias != null ? alias : columnName);
                    Document functionDoc = (Document) recurseFunctions(new Document(), f,
                            defaultFieldType, fieldNameToFieldTypeMapping);
                    document.put(key, functionDoc);
                } else {
                    throw new ParseException("Unsupported project expression");
                }
            }
            queryHolder.setProjection(document);
        }

        if (sqlCommandInfoHolder.isCountAll()) {
            queryHolder.setCountAll(true);
        }

        if (sqlCommandInfoHolder.getJoins() != null && !sqlCommandInfoHolder.getJoins().isEmpty()) {
            queryHolder.setRequiresMultistepAggregation(true);
            queryHolder.setJoinPipeline(
                    JoinProcessor.toPipelineSteps(this,
                            sqlCommandInfoHolder.getFromHolder(),
                            sqlCommandInfoHolder.getJoins(), SqlUtils.cloneExpression(
                                    sqlCommandInfoHolder.getWhereClause())));
        }

        if (sqlCommandInfoHolder.getOrderByElements() != null && sqlCommandInfoHolder.getOrderByElements().size() > 0) {
            queryHolder.setSort(createSortInfoFromOrderByElements(
                    preprocessOrderBy(sqlCommandInfoHolder.getOrderByElements(), sqlCommandInfoHolder.getFromHolder()),
                    sqlCommandInfoHolder.getAliasHolder(), sqlCommandInfoHolder.getGroupBys()));
        }

        if (sqlCommandInfoHolder.getWhereClause() != null) {
            WhereClauseProcessor whereClauseProcessor = new WhereClauseProcessor(defaultFieldType,
                    fieldNameToFieldTypeMapping, queryHolder.isRequiresMultistepAggregation());
            SQLExpr preprocessedWhere = preprocessWhere(sqlCommandInfoHolder.getWhereClause(),
                    sqlCommandInfoHolder.getFromHolder());
            if (preprocessedWhere != null) {
                //can't be null because of where of joined tables
                queryHolder.setQuery((Document) whereClauseProcessor
                        .parseExpression(new Document(), preprocessedWhere, null));
            }

            if (sqlStatement instanceof SQLUpdateStatement) {
                Document updateSetDoc = new Document();
                for (SQLUpdateSetItem updateSet : sqlCommandInfoHolder.getUpdateSets().stream().filter(sqlUpdateSetItem -> !(sqlUpdateSetItem.getValue() instanceof SQLNullExpr)).collect(Collectors.toList())) {
                    updateSetDoc.put(SqlUtils.getColumnNameFromColumn(updateSet.getColumn()),
                            SqlUtils.getNormalizedValue(updateSet.getValue(), null,
                                    defaultFieldType, fieldNameToFieldTypeMapping,
                                    sqlCommandInfoHolder.getAliasHolder(), null));
                }
                queryHolder.setUpdateSet(updateSetDoc);
                List<String> unsets = new ArrayList<>();
                for (SQLUpdateSetItem updateSet : sqlCommandInfoHolder.getUpdateSets().stream().filter(sqlUpdateSetItem -> sqlUpdateSetItem.getValue() instanceof SQLNullExpr).collect(Collectors.toList())) {
                    unsets.add(SqlUtils.getColumnNameFromColumn(updateSet.getColumn()));
                }
                queryHolder.setFieldsToUnset(unsets);
            }
        }
        if (sqlCommandInfoHolder.getHavingClause() != null) {
            HavingClauseProcessor havingClauseProcessor = new HavingClauseProcessor(defaultFieldType,
                    fieldNameToFieldTypeMapping, sqlCommandInfoHolder.getAliasHolder(),
                    queryHolder.isRequiresMultistepAggregation());
            queryHolder.setHaving((Document) havingClauseProcessor.parseExpression(new Document(),
                    sqlCommandInfoHolder.getHavingClause(), null));
        }

        queryHolder.setOffset(sqlCommandInfoHolder.getOffset());
        queryHolder.setLimit(sqlCommandInfoHolder.getLimit());

        return queryHolder;
    }

    private Object recurseFunctions(final Document query, final Object object,
                                    final FieldType defaultFieldType,
                                    final Map<String, FieldType> fieldNameToFieldTypeMapping) throws ParseException {
        if (object instanceof SQLMethodInvokeExpr) {
            SQLMethodInvokeExpr function = (SQLMethodInvokeExpr) object;
            query.put("$" + SqlUtils.translateFunctionName(function.getMethodName()),
                    recurseFunctions(new Document(), function.getArguments(),
                            defaultFieldType, fieldNameToFieldTypeMapping));
        } else if (object instanceof SQLValuesExpr) {
            SQLValuesExpr expressionList = (SQLValuesExpr) object;
            List<Object> objectList = new ArrayList<>();
            for (SQLExpr expression : expressionList.getValues()) {
                objectList.add(recurseFunctions(new Document(), expression,
                        defaultFieldType, fieldNameToFieldTypeMapping));
            }
            return objectList.size() == 1 ? objectList.get(0) : objectList;
        } else if (object instanceof SQLExpr) {
            Object normalizedValue = SqlUtils.getNormalizedValue((SQLExpr) object, null,
                    defaultFieldType, fieldNameToFieldTypeMapping, null);
            if (object instanceof SQLName) {
                return "$" + normalizedValue;
            } else {
                return normalizedValue;
            }
        }

        return query.isEmpty() ? null : query;
    }

    private SQLExpr preprocessWhere(final SQLExpr exp, final FromHolder tholder) {
        SQLExpr returnValue = exp;
        if (sqlCommandInfoHolder.getJoins() != null && !sqlCommandInfoHolder.getJoins().isEmpty()) {
            ExpressionHolder partialWhereExpHolder = new ExpressionHolder(null);
            MutableBoolean haveOrExpression = new MutableBoolean(false);
            returnValue.accept(new WhereVisitorMatchAndLookupPipelineMatchBuilder(tholder.getBaseAliasTable(),
                    partialWhereExpHolder, haveOrExpression));
            if (haveOrExpression.booleanValue()) {
                //with or exp we can't use match first step
                return null;
            }
            returnValue = partialWhereExpHolder.getExpression();
        }
        if (returnValue != null) {
            returnValue.accept(new ExpVisitorEraseAliasTableBaseBuilder(tholder.getBaseAliasTable()));
        }
        return returnValue;
    }

    private List<SQLSelectOrderByItem> preprocessOrderBy(final List<SQLSelectOrderByItem> lord, final FromHolder tholder) {
        for (SQLSelectOrderByItem ord : lord) {
            ord.accept(new ExpVisitorEraseAliasTableBaseBuilder(tholder.getBaseAliasTable()));
        }
        return lord;
    }

    private List<SQLSelectItem> preprocessSelect(final List<SQLSelectItem> lsel, final FromHolder tholder) {
        for (SQLSelectItem sel : lsel) {
            sel.accept(new ExpVisitorEraseAliasTableBaseBuilder(tholder.getBaseAliasTable()));
        }
        return lsel;
    }

    private List<String> preprocessGroupBy(final List<String> lgroup, final FromHolder tholder) {
        List<String> lgroupEraseAlias = new LinkedList<>();
        for (String group : lgroup) {
            int index = group.indexOf(tholder.getBaseAliasTable() + ".");
            if (index != -1) {
                lgroupEraseAlias.add(group.substring(tholder.getBaseAliasTable().length() + 1));
            } else {
                lgroupEraseAlias.add(group);
            }
        }
        return lgroupEraseAlias;
    }

    private Document createSortInfoFromOrderByElements(final List<SQLSelectOrderByItem> orderByElements,
                                                       final AliasHolder aliasHolder,
                                                       final List<String> groupBys) throws ParseException {
        if (orderByElements.size() == 0) {
            return new Document();
        }

        final List<SQLSelectOrderByItem> functionItems = orderByElements.stream().filter(sqlSelectOrderByItem -> sqlSelectOrderByItem.getExpr() instanceof SQLMethodInvokeExpr).collect(Collectors.toList());
        final List<SQLSelectOrderByItem> nonFunctionItems = orderByElements.stream().filter(sqlSelectOrderByItem -> !functionItems.contains(sqlSelectOrderByItem)).collect(Collectors.toList());
        Document sortItems = new Document();
        for (SQLSelectOrderByItem orderByElement : orderByElements) {
            if (nonFunctionItems.contains(orderByElement)) {
                String sortField = SqlUtils.getStringValue(orderByElement.getExpr());
                String projectField = aliasHolder.getFieldFromAliasOrField(sortField);
                if (!groupBys.isEmpty()) {

                    if (!SqlUtils.isAggregateExpression(projectField)) {
                        if (groupBys.size() > 1) {
                            projectField = "_id." + projectField.replaceAll("\\.", "_");
                        } else {
                            projectField = "_id";
                        }
                    } else {
                        projectField = sortField;
                    }
                }
                sortItems.put(projectField, orderByElement.getType() == SQLOrderingSpecification.ASC ? 1 : -1);
            } else {
                SQLMethodInvokeExpr function = (SQLMethodInvokeExpr) orderByElement.getExpr();
                String sortKey;
                String alias = aliasHolder.getAliasFromFieldExp(function.toString());
                if (alias != null && !alias.equals(function.toString())) {
                    sortKey = alias;
                } else {
                    Document parseFunctionDocument = new Document();
                    parseFunctionForAggregation(function, parseFunctionDocument,
                            Collections.<String>emptyList(), null);
                    sortKey = parseFunctionDocument.keySet().iterator().next();
                }
                sortItems.put(sortKey, orderByElement.getType() == SQLOrderingSpecification.ASC ? 1 : -1);
            }
        }

        return sortItems;
    }

    private Document createProjectionsFromSelectItems(final List<SQLSelectItem> selectItems,
                                                      final List<String> groupBys) throws ParseException {
        Document document = new Document();
        if (selectItems.size() == 0) {
            return document;
        }

        final List<SQLSelectItem> functionItems = selectItems.stream().filter(sqlSelectItem -> {
            try {
                if (!(sqlSelectItem.getExpr() instanceof SQLAllColumnExpr) && !(sqlSelectItem.getExpr() instanceof SQLAllExpr) && (sqlSelectItem.getExpr()) instanceof SQLMethodInvokeExpr) {
                    return true;
                }
            } catch (NullPointerException e) {
                return false;
            }
            return false;
        }).collect(Collectors.toList());
        final List<SQLSelectItem> nonFunctionItems = selectItems.stream().filter(sqlSelectItem -> !functionItems.contains(sqlSelectItem)).collect(Collectors.toList());

        Document idDocument = new Document();
        for (SQLSelectItem selectItem : nonFunctionItems) {
            SQLExpr column = selectItem.getExpr();
            String columnName = SqlUtils.getStringValue(column);
            idDocument.put(columnName.replaceAll("\\.", "_"), "$" + columnName);
        }

        if (!idDocument.isEmpty()) {
            document.append("_id", idDocument.size() == 1 ? idDocument.values().iterator().next() : idDocument);
        }

        for (SQLSelectItem selectItem : functionItems) {
            SQLMethodInvokeExpr function = (SQLMethodInvokeExpr) selectItem.getExpr();
            parseFunctionForAggregation(function, document, groupBys, selectItem.getAlias());
        }

        return document;
    }

    private AliasProjectionForGroupItems createAliasProjectionForGroupItems(final List<SQLSelectItem> selectItems,
                                                                            final List<String> groupBys) throws ParseException {

        AliasProjectionForGroupItems aliasProjectionForGroupItems = new AliasProjectionForGroupItems();

        final List<SQLSelectItem> functionItems = selectItems.stream().filter(sqlSelectItem -> {
            try {
                if (!(sqlSelectItem.getExpr() instanceof SQLAllColumnExpr) && !(sqlSelectItem.getExpr() instanceof SQLAllExpr) && (sqlSelectItem.getExpr()) instanceof SQLMethodInvokeExpr) {
                    return true;
                }
            } catch (NullPointerException e) {
                return false;
            }
            return false;
        }).collect(Collectors.toList());
        final List<SQLSelectItem> nonFunctionItems = selectItems.stream().filter(sqlSelectItem -> !functionItems.contains(sqlSelectItem)).collect(Collectors.toList());

        if (nonFunctionItems.size() == 1) {
            SQLSelectItem sqlSelectItem = nonFunctionItems.get(0);
            SQLExpr column = sqlSelectItem.getExpr();
            String columnName = SqlUtils.getStringValue(column);
            String alias = sqlSelectItem.getAlias();
            String nameOrAlias = (alias != null ? alias : columnName);
            aliasProjectionForGroupItems.getDocument().put(nameOrAlias, "$_id");
        } else {
            for (SQLSelectItem selectItem : nonFunctionItems) {
                SQLExpr column = selectItem.getExpr();
                String columnName = SqlUtils.getStringValue(column);
                String alias = selectItem.getAlias();
                String nameOrAlias = (alias != null ? alias : columnName);
                aliasProjectionForGroupItems.getDocument().put(nameOrAlias,
                        "$_id." + columnName.replaceAll("\\.", "_"));
            }
        }

        for (SQLSelectItem selectItem : functionItems) {
            SQLMethodInvokeExpr function = (SQLMethodInvokeExpr) selectItem.getExpr();
            String alias = selectItem.getAlias();
            Entry<String, String> fieldToAliasMapping = SqlUtils.generateAggField(function, alias);
            String aliasedField = fieldToAliasMapping.getValue();
            aliasProjectionForGroupItems.putAlias(fieldToAliasMapping.getKey(), fieldToAliasMapping.getValue());
            aliasProjectionForGroupItems.getDocument().put(aliasedField, 1);
        }

        aliasProjectionForGroupItems.getDocument().put("_id", 0);

        return aliasProjectionForGroupItems;
    }

    private void parseFunctionForAggregation(final SQLMethodInvokeExpr function, final Document document,
                                             final List<String> groupBys, final String alias) throws ParseException {
        String op = function.getMethodName().toLowerCase();
        String aggField = SqlUtils.generateAggField(function, alias).getValue();
        switch (op) {
            case "count":
                document.put(aggField, new Document("$sum", 1));
                break;
            case "sum":
            case "min":
            case "max":
            case "avg":
                createFunction(op, aggField, document, "$" + SqlUtils.getFieldFromFunction(function));
                break;
            default:
                throw new ParseException("could not understand function:" + function.getMethodName());
        }
    }

    private void createFunction(final String functionName, final String aggField,
                                final Document document, final Object value) {
        document.put(aggField, new Document("$" + functionName, value));
    }


    //Set up start pipeline, from other steps, subqueries, ...
    public List<Document> setUpStartPipeline(final MongoDBQueryHolder mongoQueryHolder) {
        List<Document> documents = mongoQueryHolder.getPrevSteps();
        if (documents == null || documents.isEmpty()) {
            documents = new LinkedList<Document>();
        }
        return documents;
    }

    public List<Document> generateAggSteps(final MongoDBQueryHolder mongoQueryHolder,
                                           final SQLCommandInfoHolder sqlCommandInfoHolder) {

        List<Document> documents = setUpStartPipeline(mongoQueryHolder);

        if (mongoQueryHolder.getQuery() != null && mongoQueryHolder.getQuery().size() > 0) {
            documents.add(new Document("$match", mongoQueryHolder.getQuery()));
        }
        if (sqlCommandInfoHolder.getJoins() != null && !sqlCommandInfoHolder.getJoins().isEmpty()) {
            documents.addAll(mongoQueryHolder.getJoinPipeline());
        }
        if (!sqlCommandInfoHolder.getGroupBys().isEmpty() || sqlCommandInfoHolder.isTotalGroup()) {
            if (mongoQueryHolder.getProjection().get("_id") == null) {
                //Generate _id with empty document
                Document dgroup = new Document();
                dgroup.put("_id", new Document());
                for (Entry<String, Object> keyValue : mongoQueryHolder.getProjection().entrySet()) {
                    if (!keyValue.getKey().equals("_id")) {
                        dgroup.put(keyValue.getKey(), keyValue.getValue());
                    }
                }
                documents.add(new Document("$group", dgroup));
            } else {
                documents.add(new Document("$group", mongoQueryHolder.getProjection()));
            }

        }
        if (mongoQueryHolder.getHaving() != null && mongoQueryHolder.getHaving().size() > 0) {
            documents.add(new Document("$match", mongoQueryHolder.getHaving()));
        }
        if (mongoQueryHolder.getSort() != null && mongoQueryHolder.getSort().size() > 0) {
            documents.add(new Document("$sort", mongoQueryHolder.getSort()));
        }
        if (mongoQueryHolder.getOffset() != -1) {
            documents.add(new Document("$skip", mongoQueryHolder.getOffset()));
        }
        if (mongoQueryHolder.getLimit() != -1) {
            documents.add(new Document("$limit", mongoQueryHolder.getLimit()));
        }

        Document aliasProjection = mongoQueryHolder.getAliasProjection();
        if (!aliasProjection.isEmpty()) {
            //Alias Group by
            documents.add(new Document("$project", aliasProjection));
        }

        if (sqlCommandInfoHolder.getGroupBys().isEmpty() && !sqlCommandInfoHolder.isTotalGroup()
                && !mongoQueryHolder.getProjection().isEmpty()) {
            //Alias no group
            Document projection = mongoQueryHolder.getProjection();
            documents.add(new Document("$project", projection));
        }

        return documents;
    }

    /**
     * Builder for {@link QueryTransformer}.
     */
    public static class Builder {

        private Boolean aggregationAllowDiskUse = null;
        private Integer aggregationBatchSize = null;
        private String sql;
        private DbType dbType;
        private Map<String, FieldType> fieldNameToFieldTypeMapping = new HashMap<>();
        private FieldType defaultFieldType = FieldType.UNKNOWN;

        /**
         * set the sql string.
         *
         * @param sql the sql string
         * @return the builder
         */
        public Builder sql(final String sql) {
            notNull(sql);
            this.sql = sql;
            return this;
        }

        /**
         * set the sql database type.
         *
         * @param dbType the sql database type
         * @return the builder
         */
        public Builder dbType(final DbType dbType) {
            this.dbType = Objects.requireNonNullElse(dbType, DbType.mysql);
            return this;
        }

        /**
         * set the column to {@link FieldType} mapping.
         *
         * @param fieldNameToFieldTypeMapping the mapping from field name to {@link FieldType}
         * @return the builder
         */
        public Builder fieldNameToFieldTypeMapping(final Map<String, FieldType> fieldNameToFieldTypeMapping) {
            notNull(fieldNameToFieldTypeMapping);
            this.fieldNameToFieldTypeMapping = fieldNameToFieldTypeMapping;
            return this;
        }

        /**
         * set the default {@link FieldType}.
         *
         * @param defaultFieldType the default {@link FieldType}
         * @return builder
         */
        public Builder defaultFieldType(final FieldType defaultFieldType) {
            notNull(defaultFieldType);
            this.defaultFieldType = defaultFieldType;
            return this;
        }

        /**
         * set whether or not aggregation is allowed to use disk use.
         *
         * @param aggregationAllowDiskUse set to true to allow disk use during aggregation
         * @return the builder
         */
        public Builder aggregationAllowDiskUse(final Boolean aggregationAllowDiskUse) {
            notNull(aggregationAllowDiskUse);
            this.aggregationAllowDiskUse = aggregationAllowDiskUse;
            return this;
        }

        /**
         * set the batch size for aggregation.
         *
         * @param aggregationBatchSize the batch size option to use for aggregation
         * @return the builder
         */
        public Builder aggregationBatchSize(final Integer aggregationBatchSize) {
            notNull(aggregationBatchSize);
            this.aggregationBatchSize = aggregationBatchSize;
            return this;
        }

        /**
         * build the {@link QueryTransformer}.
         *
         * @return the {@link QueryTransformer}
         * @throws ParseException if there was a problem processing the sql
         */
        public QueryTransformer build() throws ParseException {
            return new QueryTransformer(sql, dbType, fieldNameToFieldTypeMapping,
                    defaultFieldType, aggregationAllowDiskUse, aggregationBatchSize);
        }
    }

    private static class AliasProjectionForGroupItems {
        private final Map<String, String> fieldToAliasMapping = new HashMap<>();
        private Document document = new Document();


        public AliasHolder getFieldToAliasMapping() {
            Map<String, String> inversedMap = fieldToAliasMapping.entrySet().stream().collect(Collectors.groupingBy(Entry::getValue, Collectors.mapping(Entry::getKey, Collectors.collectingAndThen(Collectors.toList(), strings -> strings.isEmpty() ? null : strings.iterator().next()))));
            return new AliasHolder(fieldToAliasMapping, inversedMap);
        }

        public String putAlias(final String field, final String alias) {
            if (field != null) {
                return fieldToAliasMapping.put(field, alias);
            }
            return null;
        }

        public void putAll(final Map<? extends String, ? extends String> m) {
            fieldToAliasMapping.putAll(m);
        }

        public Document getDocument() {
            return document;
        }

        public AliasProjectionForGroupItems setDocument(final Document document) {
            this.document = document;
            return this;
        }
    }
}
