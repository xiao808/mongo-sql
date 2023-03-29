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
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.lang.Nullable;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static org.apache.commons.lang.Validate.notNull;

/**
 * Main class responsible for query conversion.
 */
public final class QueryConverter {
    private final Integer aggregationBatchSize;
    private final Boolean aggregationAllowDiskUse;
    private final MongoDBQueryHolder mongoDBQueryHolder;

    private final Map<String, FieldType> fieldNameToFieldTypeMapping;
    private final FieldType defaultFieldType;
    private final SQLCommandInfoHolder sqlCommandInfoHolder;

    private static final JsonWriterSettings RELAXED = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();


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
    private QueryConverter(final String sql, final Map<String, FieldType> fieldNameToFieldTypeMapping,
                           final FieldType defaultFieldType, final Boolean aggregationAllowDiskUse,
                           final Integer aggregationBatchSize) throws ParseException {
        this.aggregationAllowDiskUse = aggregationAllowDiskUse;
        this.aggregationBatchSize = aggregationBatchSize;
        SQLStatement sqlStatement = SQLUtils.parseSingleStatement(sql, DbType.mysql);
        this.defaultFieldType = defaultFieldType != null ? defaultFieldType : FieldType.UNKNOWN;
        this.sqlCommandInfoHolder = SQLCommandInfoHolder.Builder
                .create(defaultFieldType, fieldNameToFieldTypeMapping)
                .setStatement(sqlStatement)
                .build();
        this.fieldNameToFieldTypeMapping = fieldNameToFieldTypeMapping != null
                ? fieldNameToFieldTypeMapping : Collections.emptyMap();
        this.mongoDBQueryHolder = getMongoQueryInternal(sqlCommandInfoHolder);
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
    public MongoDBQueryHolder getMongoQuery() {
        return mongoDBQueryHolder;
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
        MongoDBQueryHolder mqueryHolder = getMongoQueryInternal(sqlCommandInfoHolder);
        return generateAggSteps(mqueryHolder, sqlCommandInfoHolder);
    }


    private MongoDBQueryHolder getMongoQueryInternal(final SQLCommandInfoHolder sqlCommandInfoHolder)
            throws ParseException {
        MongoDBQueryHolder mongoDBQueryHolder = new MongoDBQueryHolder(
                sqlCommandInfoHolder.getBaseTableName(), sqlCommandInfoHolder.getSqlCommandType());
        Document document = new Document();
        //From Subquery
        if (sqlCommandInfoHolder.getFromHolder().getBaseFrom().getClass() == SQLSubqueryTableSource.class) {
            mongoDBQueryHolder.setPrevSteps(fromSQLCommandInfoHolderToAggregateSteps(
                    (SQLCommandInfoHolder) sqlCommandInfoHolder.getFromHolder().getBaseSQLHolder()));
            mongoDBQueryHolder.setRequiresMultistepAggregation(true);
        }

        if (sqlCommandInfoHolder.isDistinct()) {
            document.put(sqlCommandInfoHolder.getSelectItems().get(0).toString(), 1);
            mongoDBQueryHolder.setProjection(document);
            mongoDBQueryHolder.setDistinct(true);
        } else if (sqlCommandInfoHolder.getGroupBys().size() > 0) {
            List<String> groupBys = preprocessGroupBy(sqlCommandInfoHolder.getGroupBys(),
                    sqlCommandInfoHolder.getFromHolder());
            List<SQLSelectItem> selects = preprocessSelect(sqlCommandInfoHolder.getSelectItems(),
                    sqlCommandInfoHolder.getFromHolder());
            if (sqlCommandInfoHolder.getGroupBys().size() > 0) {
                mongoDBQueryHolder.setGroupBys(groupBys);
            }
            mongoDBQueryHolder.setProjection(createProjectionsFromSelectItems(selects, groupBys));
            AliasProjectionForGroupItems aliasProjectionForGroupItems = createAliasProjectionForGroupItems(
                    selects, groupBys);
            mongoDBQueryHolder.setAliasProjection(aliasProjectionForGroupItems.getDocument());
            mongoDBQueryHolder.setRequiresMultistepAggregation(true);
        } else if (sqlCommandInfoHolder.isTotalGroup()) {
            List<SQLSelectItem> selects = preprocessSelect(sqlCommandInfoHolder.getSelectItems(),
                    sqlCommandInfoHolder.getFromHolder());
            Document d = createProjectionsFromSelectItems(selects, null);
            mongoDBQueryHolder.setProjection(d);
            AliasProjectionForGroupItems aliasProjectionForGroupItems = createAliasProjectionForGroupItems(
                    selects, null);
            sqlCommandInfoHolder.getAliasHolder().combine(aliasProjectionForGroupItems.getFieldToAliasMapping());
            mongoDBQueryHolder.setAliasProjection(aliasProjectionForGroupItems.getDocument());
        } else if (!SqlUtils.isSelectAll(sqlCommandInfoHolder.getSelectItems())) {
            document.put("_id", 0);
            for (SQLSelectItem selectItem : sqlCommandInfoHolder.getSelectItems()) {
                SQLExpr selectExpressionItem = selectItem.getExpr();
                if (selectExpressionItem instanceof SQLPropertyExpr) {
                    SQLPropertyExpr c = (SQLPropertyExpr) selectExpressionItem;
                    //If we found alias of base table we ignore it because basetable doesn't need alias, it's itself
                    String columnName = SqlUtils.removeAliasFromColumn(c, sqlCommandInfoHolder
                            .getFromHolder().getBaseAliasTable()).getName();
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
            mongoDBQueryHolder.setProjection(document);
        }

        if (sqlCommandInfoHolder.isCountAll()) {
            mongoDBQueryHolder.setCountAll(true);
        }

        if (sqlCommandInfoHolder.getJoins() != null) {
            mongoDBQueryHolder.setRequiresMultistepAggregation(true);
            mongoDBQueryHolder.setJoinPipeline(
                    JoinProcessor.toPipelineSteps(this,
                            sqlCommandInfoHolder.getFromHolder(),
                            sqlCommandInfoHolder.getJoins(), SqlUtils.cloneExpression(
                                    sqlCommandInfoHolder.getWhereClause())));
        }

        if (sqlCommandInfoHolder.getOrderByElements() != null && sqlCommandInfoHolder.getOrderByElements().size() > 0) {
            mongoDBQueryHolder.setSort(createSortInfoFromOrderByElements(
                    preprocessOrderBy(sqlCommandInfoHolder.getOrderByElements(), sqlCommandInfoHolder.getFromHolder()),
                    sqlCommandInfoHolder.getAliasHolder(), sqlCommandInfoHolder.getGroupBys()));
        }

        if (sqlCommandInfoHolder.getWhereClause() != null) {
            WhereClauseProcessor whereClauseProcessor = new WhereClauseProcessor(defaultFieldType,
                    fieldNameToFieldTypeMapping, mongoDBQueryHolder.isRequiresMultistepAggregation());
            SQLExpr preprocessedWhere = preprocessWhere(sqlCommandInfoHolder.getWhereClause(),
                    sqlCommandInfoHolder.getFromHolder());
            if (preprocessedWhere != null) {
                //can't be null because of where of joined tables
                mongoDBQueryHolder.setQuery((Document) whereClauseProcessor
                        .parseExpression(new Document(), preprocessedWhere, null));
            }

            if (SQLCommandType.UPDATE.equals(sqlCommandInfoHolder.getSqlCommandType())) {
                Document updateSetDoc = new Document();
                for (SQLUpdateSetItem updateSet : sqlCommandInfoHolder.getUpdateSets().stream().filter(sqlUpdateSetItem -> !(sqlUpdateSetItem.getValue() instanceof SQLNullExpr)).collect(Collectors.toList())) {
                    updateSetDoc.put(SqlUtils.getColumnNameFromColumn(updateSet.getColumn()),
                            SqlUtils.getNormalizedValue(updateSet.getValue(), null,
                                    defaultFieldType, fieldNameToFieldTypeMapping,
                                    sqlCommandInfoHolder.getAliasHolder(), null));
                }
                mongoDBQueryHolder.setUpdateSet(updateSetDoc);
                List<String> unsets = new ArrayList<>();
                for (SQLUpdateSetItem updateSet : sqlCommandInfoHolder.getUpdateSets().stream().filter(sqlUpdateSetItem -> sqlUpdateSetItem.getValue() instanceof SQLNullExpr).collect(Collectors.toList())) {
                    unsets.add(SqlUtils.getColumnNameFromColumn(updateSet.getColumn()));
                }
                mongoDBQueryHolder.setFieldsToUnset(unsets);
            }
        }
        if (sqlCommandInfoHolder.getHavingClause() != null) {
            HavingClauseProcessor havingClauseProcessor = new HavingClauseProcessor(defaultFieldType,
                    fieldNameToFieldTypeMapping, sqlCommandInfoHolder.getAliasHolder(),
                    mongoDBQueryHolder.isRequiresMultistepAggregation());
            mongoDBQueryHolder.setHaving((Document) havingClauseProcessor.parseExpression(new Document(),
                    sqlCommandInfoHolder.getHavingClause(), null));
        }

        mongoDBQueryHolder.setOffset(sqlCommandInfoHolder.getOffset());
        mongoDBQueryHolder.setLimit(sqlCommandInfoHolder.getLimit());

        return mongoDBQueryHolder;
    }

    protected Object recurseFunctions(final Document query, final Object object,
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

                    if (!SqlUtils.isAggregateExpression(orderByElement.getExpr())) {
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
            SQLMethodInvokeExpr function = (SQLMethodInvokeExpr)selectItem.getExpr();
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


    /**
     * get this query with supporting data in a document format.
     * The document has the following fields:
     * <pre>
     *     {
     *   "collection": "the collection the query is running on",
     *   "query": "the query (Document) for aggregation (List) needed to run this query",
     *   "commandType": "SELECT or DELETE",
     *   "countAll": "true if this is a count all Query",
     *   "distinct": "the field to do a distnct query on",
     *   "options": "A Document with the options for this aggregation",
     *   "projection": "The projection to use for this query"
     * }
     * </pre>
     * <p>
     * <p>
     * For example:
     * <pre>
     *     {
     *   "collection": "Restaurants",
     *   "query": [
     *     {
     *       "$match": {
     *         "$expr": {
     *           "$eq": [
     *             {
     *               "$toObjectId": "5e97ae59c63d1b3ff8e07c74"
     *             },
     *             "$_id"
     *           ]
     *         }
     *       }
     *     },
     *     {
     *       "$project": {
     *         "_id": 0,
     *         "id": "$_id",
     *         "R": "$Restaurant"
     *       }
     *     }
     *   ]
     * }
     * </pre>
     *
     * @return the document object.
     */
    public Document getQueryAsDocument() {
        Document retValDocument = new Document();
        MongoDBQueryHolder mongoDBQueryHolder = getMongoQuery();
        boolean isFindQuery = false;
        final String collectionName = mongoDBQueryHolder.getCollection();
        if (mongoDBQueryHolder.isDistinct()) {
            retValDocument.put("collection", collectionName);
            retValDocument.put("distinct", getDistinctFieldName(mongoDBQueryHolder));
            retValDocument.put("query", mongoDBQueryHolder.getQuery());
        } else if (sqlCommandInfoHolder.isCountAll() && !isAggregate(mongoDBQueryHolder)) {
            retValDocument.put("countAll", true);
            retValDocument.put("collection", collectionName);
            retValDocument.put("query", mongoDBQueryHolder.getQuery());
        } else if (isAggregate(mongoDBQueryHolder)) {
            retValDocument.put("collection", collectionName);
            List<Document> aggregationDocuments = generateAggSteps(mongoDBQueryHolder, sqlCommandInfoHolder);
            retValDocument.put("query", aggregationDocuments);

            Document options = new Document();
            if (aggregationAllowDiskUse != null) {
                options.put("allowDiskUse", aggregationAllowDiskUse);
            }

            if (aggregationBatchSize != null) {
                options.put("cursor", new Document("batchSize", aggregationBatchSize));
            }

            if (options.size() > 0) {
                retValDocument.put("options", options);
            }


        } else {
            retValDocument.put("commandType", sqlCommandInfoHolder.getSqlCommandType().name());
            if (sqlCommandInfoHolder.getSqlCommandType() == SQLCommandType.SELECT) {
                isFindQuery = true;
                retValDocument.put("collection", collectionName);
            } else if (Arrays.asList(SQLCommandType.DELETE, SQLCommandType.UPDATE)
                    .contains(sqlCommandInfoHolder.getSqlCommandType())) {
                retValDocument.put("collection", collectionName);
            }
            if (mongoDBQueryHolder.getUpdateSet() != null) {
                retValDocument.put("updateSet", mongoDBQueryHolder.getUpdateSet());
            }
            if (mongoDBQueryHolder.getFieldsToUnset() != null) {
                retValDocument.put("updateUnSet", mongoDBQueryHolder.getFieldsToUnset());
            }
            retValDocument.put("query", mongoDBQueryHolder.getQuery());
            if (mongoDBQueryHolder.getProjection() != null && mongoDBQueryHolder.getProjection().size() > 0
                    && sqlCommandInfoHolder.getSqlCommandType() == SQLCommandType.SELECT) {
                retValDocument.put("projection", mongoDBQueryHolder.getProjection());
            }
        }

        if (isFindQuery) {
            if (mongoDBQueryHolder.getSort() != null && mongoDBQueryHolder.getSort().size() > 0) {
                retValDocument.put("sort", mongoDBQueryHolder.getSort());
            }

            if (mongoDBQueryHolder.getOffset() != -1) {
                retValDocument.put("skip", mongoDBQueryHolder.getOffset());
            }

            if (mongoDBQueryHolder.getLimit() != -1) {
                retValDocument.put("limit", mongoDBQueryHolder.getLimit());
            }
        }

        return retValDocument;
    }

    private boolean isAggregate(final MongoDBQueryHolder mongoDBQueryHolder) {
        return (sqlCommandInfoHolder.getAliasHolder() != null
                && !sqlCommandInfoHolder.getAliasHolder().isEmpty())
                || sqlCommandInfoHolder.getGroupBys().size() > 0
                || (sqlCommandInfoHolder.getJoins() != null && sqlCommandInfoHolder.getJoins().size() > 0)
                || (mongoDBQueryHolder.getPrevSteps() != null && !mongoDBQueryHolder.getPrevSteps().isEmpty())
                || (sqlCommandInfoHolder.isTotalGroup() && !SqlUtils.isCountAll(sqlCommandInfoHolder.getSelectItems()));
    }

    private String getDistinctFieldName(final MongoDBQueryHolder mongoDBQueryHolder) {
        return mongoDBQueryHolder.getProjection().keySet().iterator().next();
    }

    /**
     * @param mongoDatabase the database to run the query against.
     * @param <T>           variable based on the type of query run.
     * @return When query does a find will return QueryResultIterator&lt;{@link Document}&gt;
     * When query does a count will return a Long
     * When query does a distinct will return QueryResultIterator&lt;{@link String}&gt;
     * @throws ParseException when the sql query cannot be parsed
     */
    @SuppressWarnings("unchecked")
    public <T> T run(final MongoDatabase mongoDatabase) throws ParseException {
        MongoDBQueryHolder mongoDBQueryHolder = getMongoQuery();

        MongoCollection mongoCollection = mongoDatabase.getCollection(mongoDBQueryHolder.getCollection());

        if (SQLCommandType.SELECT.equals(mongoDBQueryHolder.getSqlCommandType())) {

            if (mongoDBQueryHolder.isDistinct()) {
                return (T) new QueryResultIterator<>(mongoCollection.distinct(
                        getDistinctFieldName(mongoDBQueryHolder), mongoDBQueryHolder.getQuery(), String.class));
            } else if (sqlCommandInfoHolder.isCountAll() && !isAggregate(mongoDBQueryHolder)) {
                return (T) Long.valueOf(mongoCollection.countDocuments(mongoDBQueryHolder.getQuery()));
            } else if (isAggregate(mongoDBQueryHolder)) {

                AggregateIterable aggregate = mongoCollection.aggregate(
                        generateAggSteps(mongoDBQueryHolder, sqlCommandInfoHolder));

                if (aggregationAllowDiskUse != null) {
                    aggregate.allowDiskUse(aggregationAllowDiskUse);
                }

                if (aggregationBatchSize != null) {
                    aggregate.batchSize(aggregationBatchSize);
                }

                return (T) new QueryResultIterator<>(aggregate);
            } else {
                FindIterable findIterable = mongoCollection.find(mongoDBQueryHolder.getQuery())
                        .projection(mongoDBQueryHolder.getProjection());
                if (mongoDBQueryHolder.getSort() != null && mongoDBQueryHolder.getSort().size() > 0) {
                    findIterable.sort(mongoDBQueryHolder.getSort());
                }
                if (mongoDBQueryHolder.getOffset() != -1) {
                    findIterable.skip((int) mongoDBQueryHolder.getOffset());
                }
                if (mongoDBQueryHolder.getLimit() != -1) {
                    findIterable.limit((int) mongoDBQueryHolder.getLimit());
                }

                return (T) new QueryResultIterator<>(findIterable);
            }
        } else if (SQLCommandType.DELETE.equals(mongoDBQueryHolder.getSqlCommandType())) {
            DeleteResult deleteResult = mongoCollection.deleteMany(mongoDBQueryHolder.getQuery());
            return (T) ((Long) deleteResult.getDeletedCount());
        } else if (SQLCommandType.UPDATE.equals(mongoDBQueryHolder.getSqlCommandType())) {
            Document updateSet = mongoDBQueryHolder.getUpdateSet();
            List<String> fieldsToUnset = mongoDBQueryHolder.getFieldsToUnset();
            UpdateResult result = new EmptyUpdateResult();
            if ((updateSet != null && !updateSet.isEmpty()) && (fieldsToUnset != null && !fieldsToUnset.isEmpty())) {
                result = mongoCollection.updateMany(mongoDBQueryHolder.getQuery(),
                        Arrays.asList(new Document().append("$set", updateSet),
                                new Document().append("$unset", fieldsToUnset)));
            } else if (updateSet != null && !updateSet.isEmpty()) {
                result = mongoCollection.updateMany(mongoDBQueryHolder.getQuery(),
                        new Document().append("$set", updateSet));
            } else if (fieldsToUnset != null && !fieldsToUnset.isEmpty()) {
                result = mongoCollection.updateMany(mongoDBQueryHolder.getQuery(),
                        new Document().append("$unset", fieldsToUnset));
            }
            return (T) ((Long) result.getModifiedCount());
        } else {
            throw new UnsupportedOperationException("SQL command type not supported");
        }
    }

    //Set up start pipeline, from other steps, subqueries, ...
    private List<Document> setUpStartPipeline(final MongoDBQueryHolder mongoDBQueryHolder) {
        List<Document> documents = mongoDBQueryHolder.getPrevSteps();
        if (documents == null || documents.isEmpty()) {
            documents = new LinkedList<Document>();
        }
        return documents;
    }

    private List<Document> generateAggSteps(final MongoDBQueryHolder mongoDBQueryHolder,
                                            final SQLCommandInfoHolder sqlCommandInfoHolder) {

        List<Document> documents = setUpStartPipeline(mongoDBQueryHolder);

        if (mongoDBQueryHolder.getQuery() != null && mongoDBQueryHolder.getQuery().size() > 0) {
            documents.add(new Document("$match", mongoDBQueryHolder.getQuery()));
        }
        if (sqlCommandInfoHolder.getJoins() != null && !sqlCommandInfoHolder.getJoins().isEmpty()) {
            documents.addAll(mongoDBQueryHolder.getJoinPipeline());
        }
        if (!sqlCommandInfoHolder.getGroupBys().isEmpty() || sqlCommandInfoHolder.isTotalGroup()) {
            if (mongoDBQueryHolder.getProjection().get("_id") == null) {
                //Generate _id with empty document
                Document dgroup = new Document();
                dgroup.put("_id", new Document());
                for (Entry<String, Object> keyValue : mongoDBQueryHolder.getProjection().entrySet()) {
                    if (!keyValue.getKey().equals("_id")) {
                        dgroup.put(keyValue.getKey(), keyValue.getValue());
                    }
                }
                documents.add(new Document("$group", dgroup));
            } else {
                documents.add(new Document("$group", mongoDBQueryHolder.getProjection()));
            }

        }
        if (mongoDBQueryHolder.getHaving() != null && mongoDBQueryHolder.getHaving().size() > 0) {
            documents.add(new Document("$match", mongoDBQueryHolder.getHaving()));
        }
        if (mongoDBQueryHolder.getSort() != null && mongoDBQueryHolder.getSort().size() > 0) {
            documents.add(new Document("$sort", mongoDBQueryHolder.getSort()));
        }
        if (mongoDBQueryHolder.getOffset() != -1) {
            documents.add(new Document("$skip", mongoDBQueryHolder.getOffset()));
        }
        if (mongoDBQueryHolder.getLimit() != -1) {
            documents.add(new Document("$limit", mongoDBQueryHolder.getLimit()));
        }

        Document aliasProjection = mongoDBQueryHolder.getAliasProjection();
        if (!aliasProjection.isEmpty()) {
            //Alias Group by
            documents.add(new Document("$project", aliasProjection));
        }

        if (sqlCommandInfoHolder.getGroupBys().isEmpty() && !sqlCommandInfoHolder.isTotalGroup()
                && !mongoDBQueryHolder.getProjection().isEmpty()) {
            //Alias no group
            Document projection = mongoDBQueryHolder.getProjection();
            documents.add(new Document("$project", projection));
        }

        return documents;
    }

    /**
     * Builder for {@link QueryConverter}.
     */
    public static class Builder {

        private Boolean aggregationAllowDiskUse = null;
        private Integer aggregationBatchSize = null;
        private String sql;
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
         * build the {@link QueryConverter}.
         *
         * @return the {@link QueryConverter}
         * @throws ParseException if there was a problem processing the sql
         */
        public QueryConverter build() throws ParseException {
            return new QueryConverter(sql, fieldNameToFieldTypeMapping,
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

    private static class EmptyUpdateResult extends UpdateResult {
        @Override
        public boolean wasAcknowledged() {
            return false;
        }

        @Override
        public long getMatchedCount() {
            return 0;
        }

        @Override
        public long getModifiedCount() {
            return 0;
        }

        @Override
        public BsonValue getUpsertedId() {
            return null;
        }
    }
}
