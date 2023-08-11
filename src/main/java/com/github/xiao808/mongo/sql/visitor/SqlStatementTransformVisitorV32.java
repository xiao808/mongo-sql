package com.github.xiao808.mongo.sql.visitor;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLExprImpl;
import com.alibaba.druid.sql.ast.SQLLimit;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.SQLOrderingSpecification;
import com.alibaba.druid.sql.ast.expr.SQLAggregateExpr;
import com.alibaba.druid.sql.ast.expr.SQLBetweenExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLInListExpr;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;
import com.alibaba.druid.sql.ast.expr.SQLNotExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.expr.SQLValuableExpr;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectGroupByClause;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectOrderByItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectQuery;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLSubqueryTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.parser.ParserException;
import com.alibaba.druid.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.xiao808.mongo.sql.AggregateEnum;
import com.github.xiao808.mongo.sql.FunctionEnum;
import com.github.xiao808.mongo.sql.utils.SqlUtils;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.github.xiao808.mongo.sql.LexerConstants.ALL;
import static com.github.xiao808.mongo.sql.LexerConstants.AND;
import static com.github.xiao808.mongo.sql.LexerConstants.AS;
import static com.github.xiao808.mongo.sql.LexerConstants.AVG;
import static com.github.xiao808.mongo.sql.LexerConstants.COUNT;
import static com.github.xiao808.mongo.sql.LexerConstants.EQ;
import static com.github.xiao808.mongo.sql.LexerConstants.EXISTS;
import static com.github.xiao808.mongo.sql.LexerConstants.FOREIGN_FIELD;
import static com.github.xiao808.mongo.sql.LexerConstants.FROM;
import static com.github.xiao808.mongo.sql.LexerConstants.GROUP;
import static com.github.xiao808.mongo.sql.LexerConstants.GT;
import static com.github.xiao808.mongo.sql.LexerConstants.GTE;
import static com.github.xiao808.mongo.sql.LexerConstants.IN;
import static com.github.xiao808.mongo.sql.LexerConstants.LIMIT;
import static com.github.xiao808.mongo.sql.LexerConstants.LOCAL_FIELD;
import static com.github.xiao808.mongo.sql.LexerConstants.LOOKUP;
import static com.github.xiao808.mongo.sql.LexerConstants.LT;
import static com.github.xiao808.mongo.sql.LexerConstants.LTE;
import static com.github.xiao808.mongo.sql.LexerConstants.MATCH;
import static com.github.xiao808.mongo.sql.LexerConstants.MAX;
import static com.github.xiao808.mongo.sql.LexerConstants.MIN;
import static com.github.xiao808.mongo.sql.LexerConstants.NE;
import static com.github.xiao808.mongo.sql.LexerConstants.NIN;
import static com.github.xiao808.mongo.sql.LexerConstants.NOT;
import static com.github.xiao808.mongo.sql.LexerConstants.OR;
import static com.github.xiao808.mongo.sql.LexerConstants.PATH;
import static com.github.xiao808.mongo.sql.LexerConstants.PRESERVE_NULL_AND_EMPTY_ARRAYS;
import static com.github.xiao808.mongo.sql.LexerConstants.PROJECT;
import static com.github.xiao808.mongo.sql.LexerConstants.REGEX;
import static com.github.xiao808.mongo.sql.LexerConstants.ROOT;
import static com.github.xiao808.mongo.sql.LexerConstants.SKIP;
import static com.github.xiao808.mongo.sql.LexerConstants.SORT;
import static com.github.xiao808.mongo.sql.LexerConstants.SUM;
import static com.github.xiao808.mongo.sql.LexerConstants.UNWIND;
import static com.github.xiao808.mongo.sql.MongoIdConstants.CHAR_WILL_BE_REMOVED_IN_FIELD_END;
import static com.github.xiao808.mongo.sql.MongoIdConstants.CHAR_WILL_BE_REMOVED_IN_FIELD_START;
import static com.github.xiao808.mongo.sql.MongoIdConstants.DOLLAR;
import static com.github.xiao808.mongo.sql.MongoIdConstants.DOT;
import static com.github.xiao808.mongo.sql.MongoIdConstants.EMPTY_STRING;
import static com.github.xiao808.mongo.sql.MongoIdConstants.MONGO_ID;
import static com.github.xiao808.mongo.sql.MongoIdConstants.ON_CONDITION;
import static com.github.xiao808.mongo.sql.MongoIdConstants.REGEX_START_WITH;
import static com.github.xiao808.mongo.sql.MongoIdConstants.REPLACED_BY_UNDERLINE;
import static com.github.xiao808.mongo.sql.MongoIdConstants.REPRESENT_MONGO_ID;
import static com.github.xiao808.mongo.sql.MongoIdConstants.REPRESENT_PAGE_DATA;
import static com.github.xiao808.mongo.sql.MongoIdConstants.REPRESENT_PAGE_TOTAL;
import static com.github.xiao808.mongo.sql.MongoIdConstants.RIGHT_TABLE_ALIAS_OF_ON_CONDITION;
import static com.github.xiao808.mongo.sql.MongoIdConstants.SUB_QUERY_ALIAS_PLACEHOLDER;
import static com.github.xiao808.mongo.sql.MongoIdConstants.SUB_QUERY_BASE_ALIAS_PLACEHOLDER;
import static com.github.xiao808.mongo.sql.MongoIdConstants.UNDERLINE;
import static com.github.xiao808.mongo.sql.MongoIdConstants.WHERE_CONDITION_TABLE_SOURCE;

/**
 * for mongodb server version 3.2
 *
 * @author zengxiao
 * @date 2023/8/3 14:51
 * @since 1.0
 **/
public class SqlStatementTransformVisitorV32 implements SqlVisitor {

    /**
     * function to format SqlName
     * eg: SqlPropertyExpr、SqlIdentifierExpr、SQLExprTableSource.getTableName
     */
    private static final Function<String, String> FORMAT_NAME = s -> s.replaceFirst(CHAR_WILL_BE_REMOVED_IN_FIELD_START, EMPTY_STRING).replaceAll(CHAR_WILL_BE_REMOVED_IN_FIELD_END, EMPTY_STRING);
    /**
     * used to store temp result of SQL AST parsed, at special stage, temp result will be combined according to it`s relation.
     */
    private final Map<SQLObject, Document> mapping = new HashMap<>();
    /**
     * used to store temp result of SqlAggregateExpr parsed.
     */
    private final Map<SQLAggregateExpr, Document> aggregateMapping = new HashMap<>();
    /**
     * aggregation expr to select alias
     */
    private final Map<String, String> functionAliasMapping = new HashMap<>();
    /**
     * used to store temp result of SqlMethodInvokeExpr parsed.
     */
    private final Map<SQLMethodInvokeExpr, Document> functionMapping = new HashMap<>();
    /**
     * used to store local field and foreign field pair in on condition temporarily.
     */
    private String[] localForeignFieldPair = new String[2];
    /**
     * using for inner join
     */
    private final static String MATCHES = "matches";

    /**
     * whether sql select statement has join clause or group by or aggregate function except count(*)
     */
    private boolean aggregate = false;

    /**
     * whether sql select statement has distinct clause
     */
    private boolean distinct = false;
    /**
     * has $group
     */
    private boolean hasGroup = false;

    /**
     * distinct field
     */
    private String distinctField = "";

    /**
     * whether sql select statement is count(*)
     */
    private boolean countAll = false;

    @Override
    public boolean isAggregate() {
        return aggregate;
    }

    @Override
    public boolean isDistinct() {
        return distinct;
    }

    @Override
    public String getDistinctField() {
        return distinctField;
    }

    @Override
    public boolean isCountAll() {
        return countAll;
    }

    /**
     * get result parsed of any sql object
     *
     * @param sqlObject any node of sql AST, wrong argument will return empty document
     * @return result parsed of node
     */
    @Override
    public Document getDocument(SQLObject sqlObject) {
        return mapping.getOrDefault(sqlObject, new Document());
    }

    /**
     * get result parsed of where clause
     *
     * @param where where clause, wrong argument will return empty document
     * @return result parsed of where clause
     */
    @Override
    public Document getWhere(SQLExpr where) {
        return getDocument(where);
    }

    /**
     * get aggregate document list of sql select statement
     *
     * @param sqlSelectStatement sql select statement, wrong argument will return empty document
     * @return aggregate document list of sql select
     */
    @Override
    public List<Document> getAggregation(SQLSelectStatement sqlSelectStatement) {
        return getDocument(sqlSelectStatement).getList(UNDERLINE, Document.class, Collections.emptyList());
    }

    /**
     * get aggregate table name of sql select statement
     *
     * @param sqlSelectStatement sql select statement
     * @return base table name of aggregation
     */
    @Override
    public String getAggregationTableName(SQLSelectStatement sqlSelectStatement) {
        return getDocument(sqlSelectStatement).getString(FROM);
    }

    /**
     * transform select statement
     *
     * @param x sql select statement
     * @return whether execute visit method of SqlObject
     */
    @Override
    public boolean visit(SQLSelectStatement x) {
        SQLSelect select = x.getSelect();
        if (select != null) {
            SQLSelectQueryBlock queryBlock = select.getQueryBlock();
            SQLTableSource rootTableSource = queryBlock.getFrom();
            SQLSelectGroupByClause groupBy = queryBlock.getGroupBy();
            List<SQLSelectItem> selectList = queryBlock.getSelectList();
            // whether sql statement has aggregate expr
            boolean hasAggregateExpr = SqlUtils.hasAggregateOnSelectItemList(selectList);
            // when select count(*), CountDocumentsOperation will be used.
            this.countAll = SqlUtils.isCountAll(selectList);
            // when having group by or aggregate function is present in select item list, $group will be added.
            this.hasGroup = (Objects.nonNull(groupBy) || hasAggregateExpr) && !countAll;
            // is join table or has grouping by
            if (rootTableSource instanceof SQLJoinTableSource || hasGroup) {
                this.aggregate = true;
            }
            this.distinct = queryBlock.isDistinct();
            // distinct clause
            if (distinct) {
                this.distinctField = selectList.get(0).toString();
            }
            select.accept(this);
        }
        return false;
    }

    /**
     * combine query block if needed.
     *
     * @param x sql select statement
     */
    @Override
    public void endVisit(SQLSelectStatement x) {
        SQLSelect select = x.getSelect();
        // main information
        List<Document> rootAggregateList = mapping.get(select).getList(UNDERLINE, Document.class);
        SQLSelectQueryBlock queryBlock = select.getQueryBlock();
        SQLTableSource rootTableSource = queryBlock.getFrom();

        // get all sub query information, according to table alias to merge into main information.
        Map<String, Document> subQueryMap = new HashMap<>(2);
        String mainTableName = null;
        String mainTableAlias = null;
        String subQueryAlias = null;
        // get sub query information, sub query will be in the highest priority
        while (rootTableSource instanceof SQLJoinTableSource) {
            SQLTableSource right = ((SQLJoinTableSource) rootTableSource).getRight();
            if (right instanceof SQLSubqueryTableSource) {
                Document subQueryDocument = mapping.get(((SQLSubqueryTableSource) right).getSelect());
                subQueryAlias = subQueryDocument.getString(SUB_QUERY_BASE_ALIAS_PLACEHOLDER);
                mainTableAlias = subQueryDocument.getString(SUB_QUERY_ALIAS_PLACEHOLDER);
                mainTableName = subQueryDocument.getString(FROM);
                subQueryMap.put(mainTableAlias, subQueryDocument);
            }
            rootTableSource = ((SQLJoinTableSource) rootTableSource).getLeft();
        }
        if (rootTableSource instanceof SQLSubqueryTableSource) {
            Document subQueryDocument = mapping.get(((SQLSubqueryTableSource) rootTableSource).getSelect());
            subQueryAlias = subQueryDocument.getString(SUB_QUERY_BASE_ALIAS_PLACEHOLDER);
            mainTableAlias = subQueryDocument.getString(SUB_QUERY_ALIAS_PLACEHOLDER);
            mainTableName = subQueryDocument.getString(FROM);
            subQueryMap.put(mainTableAlias, subQueryDocument);
        } else if (StringUtils.isEmpty(mainTableName)) {
            // there does not have sub query and main table name is empty
            mainTableName = ((SQLExprTableSource) rootTableSource).getTableName();
        }
        boolean emptySubQueryBaseAlias = StringUtils.isEmpty(subQueryAlias);
        if (emptySubQueryBaseAlias && StringUtils.isEmpty(mainTableAlias)) {
            mainTableAlias = rootTableSource.getAlias();
        }
        // if base table has alias, add alias - $$ROOT mapping project.
        List<Document> result = new ArrayList<>();
        if (subQueryMap.isEmpty()) {
            // main table which having alias while sql without sub query
            if (!StringUtils.isEmpty(mainTableAlias)) {
                result.add(new Document(PROJECT, new Document(Map.of(mainTableAlias, ROOT, MONGO_ID, 0))));
            }
            result.addAll(rootAggregateList);
        } else {
            if (subQueryMap.size() > 1) {
                throw new IllegalArgumentException("multiple sub query is not supported now.");
            }
            // having sub query, sub query will be in the highest priority
            if (!emptySubQueryBaseAlias) {
                result.add(new Document(PROJECT, new Document(Map.of(subQueryAlias, ROOT, MONGO_ID, 0))));
            }
            Document subQuery = subQueryMap.remove(mainTableAlias);
            List<Document> tempSubQueryAggregateList = subQuery.getList(UNDERLINE, Document.class);
            // transform data of sub query to match outer alias of sub query
            List<Document> subQueryAggregateList = new ArrayList<>();
            for (Document document : tempSubQueryAggregateList) {
                Document project = document.get(PROJECT, Document.class);
                if (Objects.nonNull(project)) {
                    Document newProject = new Document();
                    for (Map.Entry<String, Object> entry : project.entrySet()) {
                        String fieldName = entry.getKey();
                        if (!emptySubQueryBaseAlias && fieldName.startsWith(subQueryAlias + DOT)) {
                            // remove sub query base alias
                            fieldName = fieldName.replaceFirst(subQueryAlias + DOT, EMPTY_STRING);
                        }
                        // add sub query alias
                        String newFieldName = mainTableAlias + DOT + fieldName;
                        newProject.put(newFieldName, entry.getValue());
                    }
                    subQueryAggregateList.add(new Document(PROJECT, newProject));
                } else {
                    subQueryAggregateList.add(document);
                }
            }
            result.addAll(subQueryAggregateList);
            result.addAll(rootAggregateList);
        }
        mapping.put(x, new Document(Map.of(UNDERLINE, result, FROM, mainTableName)));
    }

    /**
     * parse and store where clause
     *
     * @param x sql insert statement
     * @return whether execute visit method of SqlObject
     */
    @Override
    public boolean visit(SQLInsertStatement x) {
        x.getColumns().forEach(sqlExpr -> sqlExpr.accept(this));
        x.getValuesList().forEach(valuesClause -> valuesClause.getValues().forEach(sqlExpr -> sqlExpr.accept(this)));
        SQLTableSource tableSource = x.getTableSource();
        if (Objects.nonNull(tableSource)) {
            tableSource.accept(this);
        }
        return false;
    }

    /**
     * parse and store where clause
     *
     * @param x sql update statement
     * @return whether execute visit method of SqlObject
     */
    @Override
    public boolean visit(SQLUpdateStatement x) {
        x.getItems().forEach(sqlUpdateSetItem -> sqlUpdateSetItem.getColumn().accept(this));
        SQLTableSource tableSource = x.getTableSource();
        SQLTableSource from = x.getFrom();
        if (Objects.nonNull(tableSource)) {
            tableSource.accept(this);
        }
        if (Objects.nonNull(from)) {
            from.accept(this);
        }
        SQLExpr where = x.getWhere();
        if (where != null) {
            where.accept(this);
        }
        return false;
    }

    /**
     * store update information
     *
     * @param x sql update statement
     */
    @Override
    public void endVisit(SQLUpdateStatement x) {
        SQLExpr where = x.getWhere();
        mapping.put(x, new Document(Map.of(UNDERLINE, mapping.get(where), FROM, ((SQLExprTableSource) x.getTableSource()).getTableName())));
    }

    /**
     * transform delete statement
     * sub query filter is not supported now.
     *
     * @param x sql delete statement
     * @return whether execute visit method of SqlObject
     */
    @Override
    public boolean visit(SQLDeleteStatement x) {
        SQLTableSource tableSource = x.getTableSource();
        SQLTableSource from = x.getFrom();
        if (Objects.nonNull(tableSource)) {
            tableSource.accept(this);
        }
        if (Objects.nonNull(from)) {
            from.accept(this);
        }
        SQLExpr where = x.getWhere();
        if (where != null) {
            where.accept(this);
        }
        return false;
    }

    /**
     * store delete information
     *
     * @param x sql update statement
     */
    @Override
    public void endVisit(SQLDeleteStatement x) {
        SQLExpr where = x.getWhere();
        mapping.put(x, new Document(Map.of(UNDERLINE, mapping.get(where), FROM, ((SQLExprTableSource) x.getTableSource()).getTableName())));
    }

    /**
     * parse sql select
     *
     * @param x sql select
     * @return whether execute visit method of SqlObject
     */
    @Override
    public boolean visit(SQLSelect x) {
        SQLSelectQuery query = x.getQuery();
        if (query != null) {
            if (query instanceof SQLSelectQueryBlock) {
                query.accept(this);
            } else {
                throw new RuntimeException(String.format("sql select type %s is not supported.", query.getClass().getName()));
            }
        }
        return false;
    }

    /**
     * transfer data to parent
     *
     * @param x sql select
     */
    @Override
    public void endVisit(SQLSelect x) {
        SQLSelectQuery query = x.getQuery();
        // transfer data to parent.
        mapping.put(x, mapping.get(query));
    }

    /**
     * parse sql select query block
     *
     * @param x sql select query block
     * @return whether execute visit method of SqlObject
     */
    @Override
    public boolean visit(SQLSelectQueryBlock x) {
        List<SQLSelectItem> selectList = x.getSelectList();
        SQLTableSource from = x.getFrom();
        while (from instanceof SQLJoinTableSource) {
            from = ((SQLJoinTableSource) from).getLeft();
        }
        SQLTableSource resolvedTableSource = from;
        if (!StringUtils.isEmpty(from.getAlias())) {
            // field without alias will be related with first table.
            selectList.stream()
                    // field without alias
                    .filter(sqlSelectItem -> sqlSelectItem.getExpr() instanceof SQLIdentifierExpr)
                    // related with first table
                    .forEach(sqlSelectItem -> ((SQLIdentifierExpr) sqlSelectItem.getExpr()).setResolvedTableSource(resolvedTableSource));
            // add alias for SqlIdentifierExpr in group by clause
            Optional.ofNullable(x.getGroupBy()).ifPresent(group -> group.putAttribute(WHERE_CONDITION_TABLE_SOURCE, resolvedTableSource));
            // add alias for SqlIdentifierExpr in where condition
            Optional.ofNullable(x.getWhere()).ifPresent(where -> where.putAttribute(WHERE_CONDITION_TABLE_SOURCE, resolvedTableSource));
            // add alias for SqlIdentifierExpr in order by
            Optional.ofNullable(x.getOrderBy()).ifPresent(orderBy -> orderBy.getItems().stream()
                    .filter(sort -> sort.getExpr() instanceof SQLIdentifierExpr)
                    .forEach(sort -> ((SQLIdentifierExpr) sort.getExpr()).setResolvedTableSource(resolvedTableSource)));
        }
        return true;
    }

    /**
     * parse sql select query block
     * <p>
     * using aggregate handle all type of sql select query.
     * sub query select will be parsed and stored separately.
     *
     * @param x sql select query block
     */
    @Override
    public void endVisit(SQLSelectQueryBlock x) {
        SQLTableSource tableSource = x.getFrom();
        SQLObject grand = x.getParent().getParent();
        boolean isRootStatement = grand instanceof SQLSelectStatement;
        List<Document> documentList = new ArrayList<>();
        // get join document list
        List<String> tableAliasList = new ArrayList<>();
        while (tableSource instanceof SQLJoinTableSource) {
            // join must have alias, or else sql can not be parsed.
            SQLTableSource right = ((SQLJoinTableSource) tableSource).getRight();
            SQLTableSource left = ((SQLJoinTableSource) tableSource).getLeft();
            SQLJoinTableSource.JoinType joinType = ((SQLJoinTableSource) tableSource).getJoinType();
            tableAliasList.add(right.getAlias());
            if (joinType != SQLJoinTableSource.JoinType.LEFT_OUTER_JOIN) {
                SQLExpr condition = ((SQLJoinTableSource) tableSource).getCondition();
                documentList.add(new Document(MATCH, Map.of(MATCHES, Map.of(EQ, true))));
                Map<String, Object> projectMap = new HashMap<>(4);
                projectMap.put(MATCHES, mapping.get(condition));
                projectMap.put(right.getAlias(), DOLLAR + right.getAlias());
                SQLTableSource temp = left;
                while (temp instanceof SQLJoinTableSource) {
                    SQLTableSource rightAliasTable = ((SQLJoinTableSource) temp).getRight();
                    projectMap.put(rightAliasTable.getAlias(), DOLLAR + rightAliasTable.getAlias());
                    temp = ((SQLJoinTableSource) temp).getLeft();
                }
                projectMap.put(temp.getAlias(), DOLLAR + temp.getAlias());
                documentList.add(new Document(PROJECT, projectMap));
            }
            documentList.add(mapping.get(right));
            documentList.add(mapping.get(tableSource));
            tableSource = left;
        }
        if (Objects.nonNull(tableSource.getAlias())) {
            tableAliasList.add(tableSource.getAlias());
        }
        Collections.reverse(documentList);

        // get where condition information
        SQLExpr where = x.getWhere();
        if (Objects.nonNull(where)) {
            documentList.add(new Document(MATCH, mapping.get(where)));
        }

        SQLSelectGroupByClause groupBy = x.getGroupBy();
        List<SQLSelectItem> selectList = x.getSelectList();
        // when group clause is present, have to add groupBy variable and replace dot with underline.
        String groupByPrefix = DOLLAR;
        if (Objects.nonNull(groupBy)) {
            groupByPrefix += MONGO_ID + DOT;
        }
        // generate group by according to aggregate function in select item list and group by clause
        Document group = parseGroupBy(selectList, groupBy);
        if (Objects.nonNull(group)) {
            documentList.add(group);
        }
        // order by clause
        SQLOrderBy orderBy = x.getOrderBy();
        if (Objects.nonNull(orderBy)) {
            documentList.add(mapping.get(orderBy));
        }

        // format result data using project.
        documentList.addAll(parseProject(selectList, groupByPrefix, tableAliasList, isRootStatement));

        // having match clause should be added after project,
        // or else while having clause is present, the aggregation list will like this: {$group: ...}, {$match: ...}, {$project: ...}
        // order like this will make syntax error
        SQLExpr having;
        if (Objects.nonNull(groupBy) && Objects.nonNull(having = groupBy.getHaving())) {
            documentList.add(mapping.get(having));
        }

        // generate pagination according to sql limit clause
        SQLLimit limit = x.getLimit();
        parsePagination(limit, documentList);

        // use underline store collection in document
        Document query = new Document(UNDERLINE, documentList);
        if (grand instanceof SQLSubqueryTableSource && tableSource instanceof SQLExprTableSource) {
            // use from store main table name of the sub query in document
            query.put(FROM, ((SQLExprTableSource) tableSource).getTableName());
            query.put(SUB_QUERY_BASE_ALIAS_PLACEHOLDER, tableSource.getAlias());
            query.put(SUB_QUERY_ALIAS_PLACEHOLDER, ((SQLSubqueryTableSource) grand).getAlias());
        }
        mapping.put(x, query);
    }

    /**
     * parse group by and then generate group by information according to the sql select item list and group by clause.
     *
     * @param selectList sql select item list
     * @param groupBy    group by clause
     * @return group by information
     */
    private Document parseGroupBy(List<SQLSelectItem> selectList, SQLSelectGroupByClause groupBy) {
        Document groupDocument = mapping.get(groupBy);
        Map<String, Object> aggregateMap = selectList.stream()
                .filter(sqlSelectItem -> sqlSelectItem.getExpr() instanceof SQLAggregateExpr)
                .collect(Collectors.toMap(
                        sqlSelectItem -> !StringUtils.isEmpty(sqlSelectItem.getAlias()) ? sqlSelectItem.getAlias() : sqlSelectItem.getExpr().toString(),
                        sqlSelectItem -> {
                            SQLAggregateExpr aggregateExpr = (SQLAggregateExpr) sqlSelectItem.getExpr();
                            return aggregateMapping.getOrDefault(aggregateExpr, new Document());
                        }
                ));
        if (Objects.isNull(groupDocument) && aggregateMap.isEmpty()) {
            // do not need grouping by clause
            return null;
        }
        if (Objects.isNull(groupDocument)) {
            groupDocument = new Document(MONGO_ID, 1);
        }
        groupDocument.putAll(aggregateMap);
        return new Document(GROUP, groupDocument);
    }

    /**
     * parse sql select item and then generate project information according to the type of sql select item.
     *
     * @param selectList      sql select item list
     * @param groupByPrefix   prefix of group by, empty represent non-group by sql select statement.
     * @param tableAliasList  alias of all join table.
     * @param isRootStatement whether this query block is on top level.
     * @return project information
     */
    private List<Document> parseProject(List<SQLSelectItem> selectList, String groupByPrefix, List<String> tableAliasList, boolean isRootStatement) {
        List<Document> result = new ArrayList<>();
        // for select
        List<String> selectTableAliasList = new ArrayList<>();
        Map<String, Object> projectItemMap = new HashMap<>(selectList.size());
        if (SqlUtils.isSelectAll(selectList)) {
            selectTableAliasList.addAll(tableAliasList);
            projectItemMap.putAll(selectTableAliasList.stream().collect(Collectors.toMap(o -> o, s -> 0)));
        } else {
            Map<String, String> selectFieldItemMap = selectList.stream()
                    .filter(sqlSelectItem -> sqlSelectItem.getExpr() instanceof SQLName)
                    .filter(sqlSelectItem -> {
                        // select r.*, s.*
                        SQLExpr expr = sqlSelectItem.getExpr();
                        if (expr instanceof SQLPropertyExpr && ALL.equals(((SQLPropertyExpr) expr).getName())) {
                            selectTableAliasList.add(((SQLPropertyExpr) expr).getOwnerName());
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toMap(
                            sqlSelectItem -> !StringUtils.isEmpty(sqlSelectItem.getAlias()) ? sqlSelectItem.getAlias() : ((SQLName) sqlSelectItem.getExpr()).getSimpleName(),
                            sqlSelectItem -> {
                                SQLExpr expr = sqlSelectItem.getExpr();
                                String exprString = expr.toString();
                                if (expr instanceof SQLIdentifierExpr && Objects.nonNull(((SQLIdentifierExpr) expr).getResolvedTableSource())) {
                                    // for query like select name from student s left join exam e on ...
                                    // add alias of first table
                                    exprString = ((SQLIdentifierExpr) expr).getResolvedTableSource().getAlias() + DOT + exprString;
                                }
                                return exprString;
                            }
                    ));
            Map<String, SQLAggregateExpr> selectAggregateItemMap = selectList.stream()
                    .filter(sqlSelectItem -> sqlSelectItem.getExpr() instanceof SQLAggregateExpr)
                    .collect(Collectors.toMap(
                            sqlSelectItem -> !StringUtils.isEmpty(sqlSelectItem.getAlias()) ? sqlSelectItem.getAlias() : sqlSelectItem.getExpr().toString(),
                            sqlSelectItem -> (SQLAggregateExpr) sqlSelectItem.getExpr()
                    ));

            for (Map.Entry<String, String> entry : selectFieldItemMap.entrySet()) {
                // for group
                String newValue = groupByPrefix + entry.getValue();
                if (!DOLLAR.equals(groupByPrefix)) {
                    newValue = groupByPrefix + entry.getValue().replaceAll(REPLACED_BY_UNDERLINE, UNDERLINE);
                }
                projectItemMap.put(entry.getKey(), newValue);
            }
            for (Map.Entry<String, SQLAggregateExpr> entry : selectAggregateItemMap.entrySet()) {
                // aggregate method expression will be added to group by
                String selectItemAlias = entry.getKey();
                projectItemMap.put(selectItemAlias, DOLLAR + selectItemAlias);
            }
        }
        // select *
        if (selectTableAliasList.size() > 0) {
            List<String> aliasPathList = selectTableAliasList.stream().map(s -> DOLLAR + s).collect(Collectors.toList());
            if (isRootStatement) {
                aliasPathList.add(ROOT);
            }
            projectItemMap.putAll(selectTableAliasList.stream().collect(Collectors.toMap(o -> o, s -> 0)));
        }
        if (!projectItemMap.isEmpty()) {
            result.add(new Document(PROJECT, new Document(projectItemMap)));
        }
        return result;
    }

    /**
     * parse sql limit expr and then generate pagination document list according to the type of sql query block.
     *
     * @param limit        sql limit expr
     * @param documentList aggregate document list
     */
    private void parsePagination(SQLLimit limit, List<Document> documentList) {
        if (Objects.nonNull(limit)) {
            SQLValuableExpr offset = (SQLValuableExpr) limit.getOffset();
            SQLValuableExpr rowCount = (SQLValuableExpr) limit.getRowCount();
            List<Document> pagination = new ArrayList<>();
            if (offset != null) {
                pagination.add(new Document(SKIP, offset.getValue()));
            }
            pagination.add(new Document(LIMIT, rowCount.getValue()));
            documentList.addAll(pagination);
        }
    }

    /**
     * parse table join
     *
     * @param x sql order by
     * @return whether execute visit method of SqlObject
     */
    @Override
    public boolean visit(SQLJoinTableSource x) {
        SQLTableSource left = x.getLeft();
        left.accept(this);
        SQLTableSource right = x.getRight();
        right.accept(this);
        boolean rightTableSubQuery = right instanceof SQLSubqueryTableSource;
        SQLJoinTableSource.JoinType joinType = x.getJoinType();
        SQLExpr condition = x.getCondition();
        if (rightTableSubQuery) {
            condition.putAttribute(RIGHT_TABLE_ALIAS_OF_ON_CONDITION, left.getAlias());
            condition.putAttribute(ON_CONDITION, left);
            condition.accept(this);
            mapping.put(x, generateLookupStep(left, condition));
            mapping.put(right, generateUnwind(left, joinType));
        } else {
            condition.putAttribute(RIGHT_TABLE_ALIAS_OF_ON_CONDITION, right.getAlias());
            condition.putAttribute(ON_CONDITION, right);
            condition.accept(this);
            mapping.put(x, generateLookupStep(right, condition));
            mapping.put(right, generateUnwind(right, joinType));
        }
        return false;
    }

    private Document generateLookupStep(SQLTableSource rightTableSource, SQLExpr condition) {
        Document lookupInternal = new Document();
        String tableName;
        if (rightTableSource instanceof SQLExprTableSource) {
            tableName = ((SQLExprTableSource) rightTableSource).getTableName();
        } else if (rightTableSource instanceof SQLSubqueryTableSource) {
            // use empty string placeholder
            tableName = EMPTY_STRING;
        } else {
            throw new RuntimeException(String.format("table source: %s is not supported now.", rightTableSource.getClass().getSimpleName()));
        }
        lookupInternal.put(FROM, tableName);
        lookupInternal.put(LOCAL_FIELD, localForeignFieldPair[0]);
        lookupInternal.put(FOREIGN_FIELD, localForeignFieldPair[1]);
        lookupInternal.put(AS, rightTableSource.getAlias());
        localForeignFieldPair[0] = null;
        localForeignFieldPair[1] = null;
        return new Document(LOOKUP, lookupInternal);
    }

    private Document generateUnwind(SQLTableSource tableSource, SQLJoinTableSource.JoinType joinType) {
        Document unwind = new Document();
        unwind.put(UNWIND, new Document(Map.of(PATH, DOLLAR + tableSource.getAlias(), PRESERVE_NULL_AND_EMPTY_ARRAYS, joinType == SQLJoinTableSource.JoinType.LEFT_OUTER_JOIN)));
        return unwind;
    }

    /**
     * parse sub query
     *
     * @param x sub query table
     * @return whether execute visit method of SqlObject
     */
    @Override
    public boolean visit(SQLSubqueryTableSource x) {
        x.getSelect().accept(this);
        return false;
    }

    /**
     * remove quota
     *
     * @param x real table source
     * @return whether execute visit method of SqlObject
     */
    @Override
    public boolean visit(SQLExprTableSource x) {
        x.setSimpleName(FORMAT_NAME.apply(x.getTableName()));
        return false;
    }

    /**
     * parse sql select item
     *
     * @param x sql select item
     * @return whether execute visit method of SqlObject
     */
    @Override
    public boolean visit(SQLSelectItem x) {
        SQLExpr expr = x.getExpr();
        if (expr instanceof SQLMethodInvokeExpr) {
            functionAliasMapping.put(expr.toString(), x.getAlias());
        }
        expr.accept(this);
        return false;
    }

    /**
     * parse sql group by
     *
     * @param x sql group by
     * @return whether execute visit method of SqlObject
     */
    @Override
    public boolean visit(SQLSelectGroupByClause x) {
        List<SQLExpr> items = x.getItems();
        for (Map.Entry<String, Object> entry : x.getAttributes().entrySet()) {
            items.forEach(sqlExpr -> sqlExpr.putAttribute(entry.getKey(), entry.getValue()));
        }
        Document groupDocument = new Document();
        items.stream()
                .peek(sqlExpr -> sqlExpr.accept(this))
                .map(expr -> {
                    SQLTableSource resolvedTableSource;
                    String fieldString;
                    if (expr instanceof SQLIdentifierExpr && Objects.nonNull(resolvedTableSource = ((SQLIdentifierExpr) expr).getResolvedTableSource())) {
                        fieldString = resolvedTableSource.getAlias() + DOT + expr;
                    } else {
                        fieldString = expr.toString();
                    }
                    return fieldString;
                })
                .forEach(s -> groupDocument.put(s.replaceAll(REPLACED_BY_UNDERLINE, UNDERLINE), DOLLAR + s));
        mapping.put(x, new Document(MONGO_ID, groupDocument));
        SQLExpr having = x.getHaving();
        if (Objects.nonNull(having)) {
            having.accept(this);
            mapping.put(having, new Document(MATCH, mapping.get(having)));
        }
        return false;
    }

    /**
     * parse sql order by
     *
     * @param x sql order by
     * @return whether execute visit method of SqlObject
     */
    @Override
    public boolean visit(SQLOrderBy x) {
        List<SQLSelectOrderByItem> items = x.getItems();
        Document sortItems = new Document();
        for (SQLSelectOrderByItem item : items) {
            SQLExpr expr = item.getExpr();
            expr.accept(this);
            SQLOrderingSpecification type = item.getType();
            if (expr instanceof SQLPropertyExpr) {
                String sortField = expr.toString();
                sortItems.put(hasGroup ? MONGO_ID + DOT + sortField.replaceAll(REPLACED_BY_UNDERLINE, UNDERLINE) : sortField, type == SQLOrderingSpecification.ASC ? 1 : -1);
            } else if (expr instanceof SQLIdentifierExpr) {
                String sortField = expr.toString();
                SQLTableSource resolvedTableSource = ((SQLIdentifierExpr) expr).getResolvedTableSource();
                if (Objects.nonNull(resolvedTableSource)) {
                    // add alias.
                    sortField = resolvedTableSource.getAlias() + DOT + sortField;
                }
                sortItems.put(hasGroup ? MONGO_ID + DOT + sortField.replaceAll(REPLACED_BY_UNDERLINE, UNDERLINE) : sortField, type == SQLOrderingSpecification.ASC ? 1 : -1);
            } else if (expr instanceof SQLMethodInvokeExpr) {
                SQLMethodInvokeExpr function = (SQLMethodInvokeExpr) expr;
                String sortKey = parseFunction(function).keySet().iterator().next();
                sortItems.put(sortKey, type == SQLOrderingSpecification.ASC ? 1 : -1);
            }
        }
        mapping.put(x, new Document(SORT, sortItems));
        return false;
    }

    private Document parseFunction(final SQLMethodInvokeExpr function) {
        final Document document = new Document();
        String functionName = DOLLAR + function.getMethodName().toLowerCase();
        List<SQLExpr> arguments = function.getArguments();
        if (Objects.isNull(arguments) || arguments.isEmpty()) {
            throw new ParserException(String.format("function %s must have calculate field.", functionName));
        }
        String field = arguments.get(0).toString();
        String aggField;
        if (ALL.equals(field) || COUNT.equals(functionName)) {
            aggField = functionName;
        } else {
            aggField = functionName + UNDERLINE + field.replaceAll(REPLACED_BY_UNDERLINE, UNDERLINE);
        }
        aggField = aggField.substring(1);
        switch (functionName) {
            case COUNT:
                document.put(aggField, new Document(SUM, 1));
                break;
            case SUM:
            case MIN:
            case MAX:
            case AVG:
                document.put(aggField, new Document(functionName, DOLLAR + field));
                break;
            default:
                throw new RuntimeException("could not understand function:" + function.getMethodName());
        }
        return document;
    }

    /**
     * parse sql function
     *
     * @param x sql function
     * @return whether execute visit method of SqlObject
     */
    @Override
    public boolean visit(SQLMethodInvokeExpr x) {
        List<SQLExpr> arguments = x.getArguments();
        String methodName = x.getMethodName();
        if (arguments.isEmpty()) {
            throw new RuntimeException(String.format("usage of method function: %s is mistake, at least one argument is required.", methodName));
        }
        Optional<FunctionEnum> function = Arrays.stream(FunctionEnum.values()).filter(functionEnum -> functionEnum.getName().equalsIgnoreCase(methodName)).findFirst();
        if (function.isEmpty()) {
            throw new RuntimeException(String.format("method function: %s is not supported now.", methodName));
        }
        SQLExpr calculateField = arguments.get(0);
        calculateField.accept(this);
        FunctionEnum functionEnum = function.get();
        functionMapping.put(x, functionEnum.getMapper().apply(x));
        return false;
    }

    /**
     * parse sql aggregate function
     *
     * @param x sql aggregate function
     * @return whether execute visit method of SqlObject
     */
    @Override
    public boolean visit(SQLAggregateExpr x) {
        List<SQLExpr> arguments = x.getArguments();
        String methodName = x.getMethodName();
        if (arguments.size() != 1) {
            throw new RuntimeException(String.format("usage of aggregate function: %s is mistake, only one argument is permitted.", methodName));
        }
        Optional<AggregateEnum> function = Arrays.stream(AggregateEnum.values()).filter(aggregateEnum -> aggregateEnum.getName().equalsIgnoreCase(methodName)).findFirst();
        if (function.isEmpty()) {
            throw new RuntimeException(String.format("aggregate function: %s is not supported now.", methodName));
        }
        SQLExpr calculateField = arguments.get(0);
        calculateField.accept(this);
        AggregateEnum aggregateEnum = function.get();
        aggregateMapping.put(x, aggregateEnum.getMapper().apply(x));
        return false;
    }

    /**
     * parse sql in
     *
     * @param x sql in
     * @return whether execute visit method of SqlObject
     */
    @Override
    public boolean visit(final SQLInListExpr x) {
        SQLExpr expr = x.getExpr();
        for (Map.Entry<String, Object> entry : x.getAttributes().entrySet()) {
            expr.putAttribute(entry.getKey(), entry.getValue());
        }
        expr.accept(this);
        List<SQLExpr> targetList = x.getTargetList();
        List<Object> values = targetList.stream().map(sqlExpr -> ((SQLValuableExpr) sqlExpr).getValue()).collect(Collectors.toList());
        String inListExpression = x.isNot() ? NIN : IN;
        Document in = new Document();
        SQLTableSource resolvedTableSource;
        String fieldString;
        if (expr instanceof SQLIdentifierExpr && Objects.nonNull(resolvedTableSource = ((SQLIdentifierExpr) expr).getResolvedTableSource())) {
            fieldString = resolvedTableSource.getAlias() + DOT + expr;
        } else {
            fieldString = expr.toString();
        }
        in.put(fieldString, Map.of(inListExpression, values));
        mapping.put(x, in);
        return false;
    }

    /**
     * parse between
     *
     * @param x sql between
     * @return whether execute visit method of SqlObject
     */
    @Override
    public boolean visit(final SQLBetweenExpr x) {
        SQLExpr testExpr = x.getTestExpr();
        SQLExpr beginExpr = x.getBeginExpr();
        SQLExpr endExpr = x.getEndExpr();
        SQLBinaryOpExpr start = new SQLBinaryOpExpr(testExpr, SQLBinaryOperator.GreaterThanOrEqual, beginExpr);
        SQLBinaryOpExpr end = new SQLBinaryOpExpr(testExpr, SQLBinaryOperator.LessThanOrEqual, endExpr);
        SQLExprImpl left = x.isNot() ? new SQLNotExpr(start) : start;
        SQLExprImpl right = x.isNot() ? new SQLNotExpr(end) : end;
        SQLBinaryOpExpr andExpression = new SQLBinaryOpExpr(left, SQLBinaryOperator.BooleanAnd, right);
        for (Map.Entry<String, Object> entry : x.getAttributes().entrySet()) {
            andExpression.putAttribute(entry.getKey(), entry.getValue());
        }
        this.visit(andExpression);
        mapping.get(andExpression);
        mapping.put(x, new Document(AND, List.of(mapping.get(left), mapping.get(right))));
        return false;
    }

    /**
     * parse not
     *
     * @param x sql not
     * @return whether execute visit method of SqlObject
     */
    @Override
    public boolean visit(final SQLNotExpr x) {
        SQLExpr expression = x.getExpr();
        for (Map.Entry<String, Object> entry : x.getAttributes().entrySet()) {
            expression.putAttribute(entry.getKey(), entry.getValue());
        }
        if (expression instanceof SQLName) {
            mapping.put(x, new Document(expression.toString(), new Document(NE, true)));
        } else if (expression instanceof SQLBinaryOpExpr) {
            this.visit((SQLBinaryOpExpr) expression);
            Document parsedDocument = mapping.get(expression);
            String column = parsedDocument.keySet().iterator().next();
            Document value = parsedDocument.get(column, Document.class);
            mapping.put(x, new Document(column, new Document(NOT, value)));
        }
        return false;
    }

    /**
     * parse sql comparator
     *
     * @param x sql comparator
     * @return whether execute visit method of SqlObject
     */
    @Override
    public boolean visit(SQLBinaryOpExpr x) {
        final SQLBinaryOperator op = x.getOperator();
        final SQLExpr left = x.getLeft();
        SQLExpr right = x.getRight();
        // remove alias and generate let document according to on attribute.
        // this happens only when sql select query block has join clause.
        for (Map.Entry<String, Object> entry : x.getAttributes().entrySet()) {
            left.putAttribute(entry.getKey(), entry.getValue());
            right.putAttribute(entry.getKey(), entry.getValue());
        }
        Object rightTableAlias = x.getAttribute(RIGHT_TABLE_ALIAS_OF_ON_CONDITION);
        Object onCondition = x.getAttribute(ON_CONDITION);
        boolean isOnCondition = Objects.nonNull(rightTableAlias) && Objects.nonNull(onCondition);
        boolean isNotFiledPair = StringUtils.isEmpty(localForeignFieldPair[0]) || StringUtils.isEmpty(localForeignFieldPair[1]);
        if (isOnCondition && isNotFiledPair) {
            // clear
            localForeignFieldPair[0] = null;
            localForeignFieldPair[1] = null;
        }
        String column = handleLeft(left, isOnCondition, onCondition, rightTableAlias);
        Object value = handleRight(right, isOnCondition, onCondition, rightTableAlias);

        if (Objects.nonNull(column) && Objects.nonNull(value)) {
            mapping.put(x, handleBinaryOp(op, column, value, isOnCondition));
        }

        if (op == SQLBinaryOperator.BooleanAnd || op == SQLBinaryOperator.BooleanOr) {
            mapping.put(x, new Document());
        }
        return true;
    }

    private String handleLeft(final SQLExpr left, boolean isOnCondition, Object onCondition, Object rightTableAlias) {
        String column = null;
        if (left != null) {
            if (left instanceof SQLBinaryOpExpr) {
                this.visit((SQLBinaryOpExpr) left);
            } else if (left instanceof SQLPropertyExpr) {
                left.accept(this);
                String leftExpression = left.toString();
                if (isOnCondition) {
                    // for on condition
                    if (((SQLPropertyExpr) left).getOwnerName().equals(rightTableAlias)) {
                        // field belongs to right table.
                        if (StringUtils.isEmpty(localForeignFieldPair[1])) {
                            localForeignFieldPair[1] = ((SQLName) left).getSimpleName();
                        }
                    } else {
                        // field belongs to left table.
                        if (StringUtils.isEmpty(localForeignFieldPair[0])) {
                            localForeignFieldPair[0] = leftExpression;
                        }
                    }
                    column = DOLLAR + leftExpression;
                } else {
                    // for where condition
                    column = leftExpression;
                }
            } else if (left instanceof SQLIdentifierExpr) {
                // only for where condition, join condition must have table alias present.
                left.accept(this);
                SQLTableSource resolvedTableSource = ((SQLIdentifierExpr) left).getResolvedTableSource();
                if (Objects.nonNull(resolvedTableSource) && !StringUtils.isEmpty(resolvedTableSource.getAlias())) {
                    column = resolvedTableSource.getAlias() + DOT + ((SQLIdentifierExpr) left).getSimpleName();
                } else {
                    column = left.toString();
                }
            } else {
                left.accept(this);
                column = functionAliasMapping.get(left.toString());
            }
        }
        return column;
    }

    private Object handleRight(final SQLExpr right, boolean isOnCondition, Object onCondition, Object rightTableAlias) {
        Object value = null;
        if (right != null) {
            if (right instanceof SQLBinaryOpExpr) {
                this.visit((SQLBinaryOpExpr) right);
            } else if (right instanceof SQLPropertyExpr) {
                right.accept(this);
                if (isOnCondition) {
                    String rightExpression = right.toString();
                    // for on condition
                    if (((SQLPropertyExpr) right).getOwnerName().equals(rightTableAlias)) {
                        // field belongs to right table.
                        if (StringUtils.isEmpty(localForeignFieldPair[1])) {
                            localForeignFieldPair[1] = ((SQLName) right).getSimpleName();
                        }
                    } else {
                        // field belongs to left table.
                        if (StringUtils.isEmpty(localForeignFieldPair[0])) {
                            localForeignFieldPair[0] = rightExpression;
                        }
                    }
                    value = new SQLIdentifierExpr(rightExpression);
                } else {
                    // for where condition
                    value = right;
                }
            } else if (right instanceof SQLIdentifierExpr) {
                right.accept(this);
                SQLTableSource resolvedTableSource = ((SQLIdentifierExpr) right).getResolvedTableSource();
                if (Objects.nonNull(resolvedTableSource) && !StringUtils.isEmpty(resolvedTableSource.getAlias())) {
                    // for query like select name from student s
                    value = new SQLPropertyExpr(resolvedTableSource.getAlias(), ((SQLIdentifierExpr) right).getName());
                } else {
                    value = right;
                }
            } else if (right instanceof SQLValuableExpr) {
                value = right;
            } else {
                right.accept(this);
            }
        }
        return value;
    }

    private Document handleBinaryOp(SQLBinaryOperator operator, String column, Object value, boolean isOnCondition) {
        Document query = new Document();
        Object rightExpression = value instanceof SQLName ? DOLLAR + value : ((SQLValuableExpr) value).getValue();
        switch (operator) {
            case Equality:
                if (isOnCondition) {
                    query.put(EQ, List.of(column, rightExpression));
                } else {
                    query.put(column, Map.of(EQ, rightExpression));
                }
                break;
            case NotEqual:
                if (isOnCondition) {
                    query.put(NE, List.of(column, rightExpression));
                } else {
                    query.put(column, Map.of(NE, rightExpression));
                }
                break;
            case GreaterThan:
                if (isOnCondition) {
                    query.put(GT, List.of(column, rightExpression));
                } else {
                    query.put(column, Map.of(GT, rightExpression));
                }
                break;
            case LessThan:
                if (isOnCondition) {
                    query.put(LT, List.of(column, rightExpression));
                } else {
                    query.put(column, Map.of(LT, rightExpression));
                }
                break;
            case GreaterThanOrEqual:
                if (isOnCondition) {
                    query.put(GTE, List.of(column, rightExpression));
                } else {
                    query.put(column, Map.of(GTE, rightExpression));
                }
                break;
            case LessThanOrEqual:
                if (isOnCondition) {
                    query.put(LTE, List.of(column, rightExpression));
                } else {
                    query.put(column, Map.of(LTE, rightExpression));
                }
                break;
            case Is:
                query.put(column, new Document(EXISTS, true));
                break;
            case IsNot:
                query.put(column, new Document(EXISTS, false));
                break;
            case Like:
                query.put(column, Map.of(REGEX, REGEX_START_WITH + SqlUtils.constructLikeRegex(rightExpression.toString()) + DOLLAR));
                break;
            case NotLike:
                query.put(column, new Document(Map.of(NOT, Pattern.compile(REGEX_START_WITH + SqlUtils.constructLikeRegex(rightExpression.toString()) + DOLLAR))));
                break;
            default:
        }
        return query;
    }

    @Override
    public void endVisit(SQLBinaryOpExpr x) {
        SQLBinaryOperator operator = x.getOperator();
        if (operator == SQLBinaryOperator.BooleanOr || operator == SQLBinaryOperator.BooleanAnd) {
            SQLExpr left = x.getLeft();
            SQLExpr right = x.getRight();
            Document document = mapping.get(x);
            document.put(operator == SQLBinaryOperator.BooleanOr ? OR : AND, List.of(mapping.get(left), mapping.get(right)));
        }
    }

    /**
     * remove quota
     *
     * @param x sql property
     * @return whether execute visit method of SqlObject
     */
    @Override
    public boolean visit(SQLPropertyExpr x) {
        x.setName(FORMAT_NAME.apply(x.getName()));
        if (REPRESENT_MONGO_ID.equalsIgnoreCase(x.getName())) {
            x.setName(MONGO_ID);
        }
        return false;
    }

    /**
     * remove quota
     *
     * @param x sql identifier
     * @return whether execute visit method of SqlObject
     */
    @Override
    public boolean visit(SQLIdentifierExpr x) {
        x.setName(FORMAT_NAME.apply(x.getName()));
        if (REPRESENT_MONGO_ID.equalsIgnoreCase(x.getName())) {
            x.setName(MONGO_ID);
        }
        Object whereConditionTableSource = x.getAttribute(WHERE_CONDITION_TABLE_SOURCE);
        if (Objects.nonNull(whereConditionTableSource)) {
            x.setResolvedTableSource((SQLTableSource) whereConditionTableSource);
        }
        return false;
    }

    @Override
    public JsonNode executeSelect(SQLSelectStatement sqlSelectStatement, MongoDatabase mongoDatabase, boolean aggregationAllowDiskUse, int aggregationBatchSize) {
        sqlSelectStatement.accept(this);
        String tableName = this.getAggregationTableName(sqlSelectStatement);
        MongoCollection<ObjectNode> collection = mongoDatabase.getCollection(tableName, ObjectNode.class);
        if (this.isDistinct()) {
            // select distinct
            return new DistinctOperation().execute(sqlSelectStatement, collection);
        } else if (this.isCountAll() && !this.isAggregate()) {
            // select count(*)
            return new CountOperation().execute(sqlSelectStatement, collection, aggregationAllowDiskUse, aggregationBatchSize);
        } else {
            // aggregation
            return new AggregateOperation().execute(sqlSelectStatement, collection, aggregationAllowDiskUse, aggregationBatchSize);
        }
    }


    /**
     * distinct
     */
    class DistinctOperation {

        public JsonNode execute(SQLSelectStatement selectStatement, MongoCollection<ObjectNode> collection) {
            SQLSelectQueryBlock queryBlock = selectStatement.getSelect().getQueryBlock();
            String tableAlias = queryBlock.getFrom().getAlias();
            Document filter = SqlStatementTransformVisitorV32.this.getWhere(queryBlock.getWhere());
            String distinctField = SqlStatementTransformVisitorV32.this.getDistinctField().replace(tableAlias + DOT, EMPTY_STRING);
            if (filter != null && !filter.isEmpty()) {
                filter = Document.parse(filter.toJson().replace(tableAlias + DOT, EMPTY_STRING));
            }
            DistinctIterable<String> distinctIterable = collection.distinct(distinctField, filter, String.class);
            ArrayNode result = new ArrayNode(JsonNodeFactory.instance);
            for (String data : distinctIterable) {
                result.add(data);
            }
            return result;
        }
    }

    /**
     * aggregation
     */
    class AggregateOperation {

        public JsonNode execute(SQLSelectStatement selectStatement, MongoCollection<ObjectNode> collection, boolean aggregationAllowDiskUse, int aggregationBatchSize) {
            SQLSelectQueryBlock queryBlock = selectStatement.getSelect().getQueryBlock();
            SQLLimit limit = queryBlock.getLimit();
            List<Document> aggregation = SqlStatementTransformVisitorV32.this.getAggregation(selectStatement);
            if (Objects.nonNull(limit)) {
                List<Document> countDocument = aggregation.stream().filter(document -> !document.containsKey(SKIP) && !document.containsKey(LIMIT)).collect(Collectors.toList());
                countDocument.add(new Document(GROUP, new Document(MONGO_ID, new BsonInt32(1))
                        .append(REPRESENT_PAGE_TOTAL, new Document(SUM, new BsonInt32(1)))));
                long total = 0L;
                try (MongoCursor<ObjectNode> cursor = collection.aggregate(countDocument)
                        .allowDiskUse(aggregationAllowDiskUse)
                        .batchSize(aggregationBatchSize)
                        .iterator()) {
                    ObjectNode result = cursor.hasNext() ? cursor.next() : null;
                    if (result == null || result.isEmpty()) {
                        return new ObjectNode(JsonNodeFactory.instance).put(REPRESENT_PAGE_TOTAL, total);
                    } else {
                        total = result.get(REPRESENT_PAGE_TOTAL).longValue();
                        AggregateIterable<ObjectNode> aggregate = collection.aggregate(aggregation)
                                .allowDiskUse(aggregationAllowDiskUse)
                                .batchSize(aggregationBatchSize);
                        ArrayNode data = unwrapResult(aggregate);
                        return new ObjectNode(JsonNodeFactory.instance).put(REPRESENT_PAGE_TOTAL, total).set(REPRESENT_PAGE_DATA, data);
                    }
                }
            } else {
                AggregateIterable<ObjectNode> aggregate = collection.aggregate(aggregation)
                        .allowDiskUse(aggregationAllowDiskUse)
                        .batchSize(aggregationBatchSize);
                return unwrapResult(aggregate);
            }
        }

        /**
         * unwrap result
         *
         * @param aggregate aggregation result iterable
         * @return aggregate result
         */
        private ArrayNode unwrapResult(AggregateIterable<ObjectNode> aggregate) {
            ArrayNode result = new ArrayNode(JsonNodeFactory.instance);
            for (ObjectNode node : aggregate) {
                result.add(node);
            }
            return result;
        }

        /**
         * unwrap result
         *
         * @param aggregate aggregation result iterable
         * @return aggregate result
         */
        private ObjectNode unwrapPaginationResult(AggregateIterable<ObjectNode> aggregate) {
            try (MongoCursor<ObjectNode> cursor = aggregate.iterator()) {
                return cursor.hasNext() ? cursor.next() : new ObjectNode(JsonNodeFactory.instance);
            }
        }
    }

    /**
     * select count(*)
     */
    class CountOperation {

        public LongNode execute(SQLSelectStatement selectStatement, MongoCollection<ObjectNode> collection, boolean aggregationAllowDiskUse, int aggregationBatchSize) {
            SQLSelectQueryBlock queryBlock = selectStatement.getSelect().getQueryBlock();
            String alias = queryBlock.getFrom().getAlias();
            SQLLimit limit = queryBlock.getLimit();
            List<Document> aggregation = new ArrayList<>();
            if (!StringUtils.isEmpty(alias)) {
                aggregation.add(new Document(PROJECT, new Document(Map.of(alias, ROOT, MONGO_ID, 0))));
            }
            Document filter = SqlStatementTransformVisitorV32.this.getWhere(queryBlock.getWhere());
            aggregation.add(new Document(MATCH, filter != null ? filter : new BsonDocument()));
            if (Objects.nonNull(limit)) {
                Long skip = (Long) SqlUtils.getRealValue(limit.getOffset());
                Long size = (Long) SqlUtils.getRealValue(limit.getRowCount());
                if (Objects.nonNull(skip) && skip > 0) {
                    aggregation.add(new Document(SKIP, new BsonInt64(skip)));
                }
                if (Objects.nonNull(size) && size > 0) {
                    aggregation.add(new Document(LIMIT, new BsonInt64(size)));
                }
            }
            aggregation.add(new Document(GROUP, new Document(MONGO_ID, new BsonInt32(1))
                    .append(REPRESENT_PAGE_TOTAL, new Document(SUM, new BsonInt32(1)))));
            try (MongoCursor<ObjectNode> cursor = collection.aggregate(aggregation)
                    .allowDiskUse(aggregationAllowDiskUse)
                    .batchSize(aggregationBatchSize)
                    .iterator()) {
                return new LongNode(cursor.hasNext() ? unwrapResult(cursor.next()) : 0);
            }
        }

        /**
         * unwrap result
         *
         * @param result aggregation result
         * @return count result
         */
        private Long unwrapResult(final ObjectNode result) {
            if (result == null || result.isEmpty()) {
                return 0L;
            } else {
                return result.get(REPRESENT_PAGE_TOTAL).longValue();
            }
        }
    }

}

