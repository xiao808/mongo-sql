package com.github.xiao808.mongo.sql.processor;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSubqueryTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.github.xiao808.mongo.sql.FieldType;
import com.github.xiao808.mongo.sql.ParseException;
import com.github.xiao808.mongo.sql.QueryTransformer;
import com.github.xiao808.mongo.sql.holder.ExpressionHolder;
import com.github.xiao808.mongo.sql.holder.from.FromHolder;
import com.github.xiao808.mongo.sql.holder.from.SQLCommandInfoHolder;
import com.github.xiao808.mongo.sql.visitor.ExpVisitorEraseAliasTableBaseBuilder;
import com.github.xiao808.mongo.sql.visitor.OnVisitorLetsBuilder;
import com.github.xiao808.mongo.sql.visitor.OnVisitorMatchLookupBuilder;
import com.github.xiao808.mongo.sql.visitor.WhereVisitorMatchAndLookupPipelineMatchBuilder;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.bson.Document;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Class used to help with sql joins.
 */
public final class JoinProcessor {

    private JoinProcessor() {

    }

    private static Document generateLetsFromON(final FromHolder tholder,
                                               final SQLExpr onExp, final String aliasTableName) {
        Document onDocument = new Document();
        onExp.accept(new OnVisitorLetsBuilder(onDocument, aliasTableName, tholder.getBaseAliasTable()));
        return onDocument;
    }

    private static Document generateMatchJoin(final FromHolder tholder, final SQLExpr onExp,
                                              final SQLExpr wherePartialExp,
                                              final String joinTableAlias) throws ParseException {
        Document matchJoinStep = new Document();
        onExp.accept(new OnVisitorMatchLookupBuilder(joinTableAlias, tholder.getBaseAliasTable()));
        WhereClauseProcessor whereClauseProcessor = new WhereClauseProcessor(FieldType.UNKNOWN,
                Collections.<String, FieldType>emptyMap(), true);

        matchJoinStep.put("$match", whereClauseProcessor
                .parseExpression(new Document(), wherePartialExp != null
                        ? new SQLBinaryOpExpr(onExp, SQLBinaryOperator.BooleanAnd, wherePartialExp) : onExp, null));
        return matchJoinStep;
    }

    private static List<Document> generateSubPipelineLookup(final FromHolder tholder, final SQLExpr onExp,
                                                            final SQLExpr wherePartialExp,
                                                            final String aliasTableName,
                                                            final List<Document> subqueryDocs) throws ParseException {
        List<Document> ldoc = subqueryDocs;
        ldoc.add(generateMatchJoin(tholder, onExp, wherePartialExp, aliasTableName));
        return ldoc;
    }

    private static Document generateInternalLookup(final FromHolder tholder, final String joinTableName,
                                                   final String joinTableAlias, final SQLExpr onExp,
                                                   final SQLExpr wherePartialExp,
                                                   final List<Document> subqueryDocs) throws ParseException {
        Document lookupInternal = new Document();
        lookupInternal.put("from", joinTableName);
        lookupInternal.put("let", generateLetsFromON(tholder, onExp, joinTableAlias));
        lookupInternal.put("pipeline", generateSubPipelineLookup(tholder, onExp,
                wherePartialExp, joinTableAlias, subqueryDocs));
        lookupInternal.put("as", joinTableAlias);

        return lookupInternal;
    }

    /**
     * Will perform an lookup step. Like this:
     * <pre>
     *      {
     *                "$lookup":{
     *                    "from": "rightCollection",
     *                    "let": {
     *                     left collection ON fields
     *                 },
     *                 "pipeline": [
     *                     {
     *                      "$match": {
     *                           whereClaseForOn
     *                       }
     *                     }
     *                 ],
     *                 "as": ""
     *                 }
     *               }
     * </pre>
     *
     * @param tholder            the {@link FromHolder}
     * @param joinTableName      the join table name
     * @param joinTableAlias     the alias for the join table
     * @param onExp              {@link SQLExpr}
     * @param mixedOnAndWhereExp the mixed on and where {@link SQLExpr}
     * @param subqueryDocs       the sub query {@link Document}s
     * @return the lookup step
     * @throws ParseException if there is an issue parsing the sql
     */
    private static Document generateLookupStep(final FromHolder tholder, final String joinTableName,
                                               final String joinTableAlias, final SQLExpr onExp,
                                               final SQLExpr mixedOnAndWhereExp,
                                               final List<Document> subqueryDocs) throws ParseException {
        Document lookup = new Document();
        lookup.put("$lookup", generateInternalLookup(tholder, joinTableName,
                joinTableAlias, onExp, mixedOnAndWhereExp, subqueryDocs));
        return lookup;
    }

    private static Document generateUnwindInternal(final FromHolder tholder,
                                                   final String joinTableAlias, final boolean isLeft) {
        Document internalUnwind = new Document();
        internalUnwind.put("path", "$" + joinTableAlias);
        internalUnwind.put("preserveNullAndEmptyArrays", isLeft);
        return internalUnwind;
    }

    /**
     * Will create an unwind step.  Like this:
     * <pre>
     *        {
     *                "$unwind":{
     *                "path": "fieldtounwind",
     *                "preserveNullAndEmptyArrays": (true for leftjoin false inner)
     *             }
     *          }
     * </pre>
     *
     * @param tholder        the {@link FromHolder}
     * @param joinTableAlias the alias of the join table
     * @param isLeft         true if is a left join
     * @return the unwind step
     * @throws ParseException if there is an issue parsing the sql
     */
    private static Document generateUnwindStep(final FromHolder tholder,
                                               final String joinTableAlias,
                                               final boolean isLeft) throws ParseException {
        Document unwind = new Document();
        unwind.put("$unwind", generateUnwindInternal(tholder, joinTableAlias, isLeft));
        return unwind;
    }

    private static Document generateInternalMatchAfterJoin(final String baseAliasTable,
                                                           final SQLExpr whereExpression) throws ParseException {
        WhereClauseProcessor whereClauseProcessor = new WhereClauseProcessor(FieldType.UNKNOWN,
                Collections.<String, FieldType>emptyMap());

        whereExpression.accept(new ExpVisitorEraseAliasTableBaseBuilder(baseAliasTable));

        return (Document) whereClauseProcessor
                .parseExpression(new Document(), whereExpression, null);
    }

    /**
     * Will generate unwind step.  Like this:
     * <pre>
     *  {
     *               "$unwind":{
     *                  "path": "fieldtounwind",
     *                  "preserveNullAndEmptyArrays": (true for leftjoin false inner)
     *              }
     *          }
     * </pre>
     *
     * @param tholder         the {@link FromHolder}
     * @param whereExpression the where expression from the query
     * @return the unwind step
     * @throws ParseException if there is an issue parsing the sql
     */
    private static Document generateMatchAfterJoin(final FromHolder tholder,
                                                   final SQLExpr whereExpression) throws ParseException {
        Document match = new Document();
        match.put("$match", generateInternalMatchAfterJoin(tholder.getBaseAliasTable(), whereExpression));
        return match;
    }

    /**
     * Create the aggregation pipeline steps needed to perform a join.
     *
     * @param queryTransformer the {@link QueryTransformer}
     * @param tholder          the {@link FromHolder}
     * @param ljoins           the list of joined tables
     * @param whereExpression  the where expression from the query
     * @return the aggregation pipeline steps
     * @throws ParseException if there is an issue parsing the sql
     */
    public static List<Document> toPipelineSteps(final QueryTransformer queryTransformer,
                                                 final FromHolder tholder, final List<SQLTableSource> ljoins,
                                                 final SQLExpr whereExpression)
            throws ParseException {
        List<Document> ldoc = new LinkedList<Document>();
        MutableBoolean haveOrExpression = new MutableBoolean();
        for (SQLTableSource j : ljoins) {
            SQLJoinTableSource tableSource = (SQLJoinTableSource) j;
            if (tableSource.getJoinType() == SQLJoinTableSource.JoinType.INNER_JOIN || tableSource.getJoinType() == SQLJoinTableSource.JoinType.LEFT_OUTER_JOIN) {

                if (tableSource.getRight() instanceof SQLExprTableSource || tableSource.getRight() instanceof SQLSubqueryTableSource) {
                    ExpressionHolder whereExpHolder;
                    String joinTableAlias = tableSource.getRight().getAlias();
                    String joinTableName = tholder.getSQLHolder(tableSource.getRight()).getBaseTableName();

                    whereExpHolder = new ExpressionHolder(null);

                    if (whereExpression != null) {
                        haveOrExpression.setValue(false);
                        whereExpression.accept(new WhereVisitorMatchAndLookupPipelineMatchBuilder(joinTableAlias,
                                whereExpHolder, haveOrExpression));
                        if (!haveOrExpression.booleanValue() && whereExpHolder.getExpression() != null) {
                            whereExpHolder.getExpression().accept(
                                    new ExpVisitorEraseAliasTableBaseBuilder(joinTableAlias));
                        } else {
                            whereExpHolder.setExpression(null);
                        }
                    }

                    List<Document> subqueryDocs = new LinkedList<>();

                    if (tableSource.getRight() instanceof SQLSubqueryTableSource) {
                        subqueryDocs = queryTransformer.fromSQLCommandInfoHolderToAggregateSteps(
                                (SQLCommandInfoHolder) tholder.getSQLHolder(tableSource.getRight()));
                    }

                    ldoc.add(generateLookupStep(tholder, joinTableName,
                            joinTableAlias, tableSource.getCondition(), whereExpHolder.getExpression(), subqueryDocs));
                    ldoc.add(generateUnwindStep(tholder, joinTableAlias, tableSource.getJoinType() == SQLJoinTableSource.JoinType.LEFT_OUTER_JOIN));
                } else {
                    throw new ParseException("From join not supported");
                }
            } else {
                throw new ParseException("Only inner join and left supported");
            }

        }
        if (haveOrExpression.booleanValue()) {
            //if there is some "or" we use this step for support this logic and no other match steps
            ldoc.add(generateMatchAfterJoin(tholder, whereExpression));
        }
        return ldoc;
    }

}
