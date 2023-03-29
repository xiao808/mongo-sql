package com.github.xiao808.mongo.sql.processor;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.expr.SQLBetweenExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.expr.SQLInListExpr;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;
import com.alibaba.druid.sql.ast.expr.SQLNotExpr;
import com.alibaba.druid.sql.ast.expr.SQLNullExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.expr.SQLValuableExpr;
import com.alibaba.druid.sql.ast.expr.SQLValuesExpr;
import com.alibaba.druid.sql.ast.statement.SQLSubqueryTableSource;
import com.github.xiao808.mongo.sql.FieldType;
import com.github.xiao808.mongo.sql.ParseException;
import com.github.xiao808.mongo.sql.holder.AliasHolder;
import com.github.xiao808.mongo.sql.utils.DateFunction;
import com.github.xiao808.mongo.sql.utils.ObjectIdFunction;
import com.github.xiao808.mongo.sql.utils.RegexFunction;
import com.github.xiao808.mongo.sql.utils.SqlUtils;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Class responsible for parsing where clause in sql structure.
 */
public class WhereClauseProcessor {

    private final FieldType defaultFieldType;
    private final Map<String, FieldType> fieldNameToFieldTypeMapping;
    private final boolean requiresMultistepAggregation;
    @SuppressWarnings("checkstyle:VisibilityModifier")
    protected AliasHolder aliasHolder;

    /**
     * Default constructor.
     *
     * @param defaultFieldType             the default {@link FieldType}
     * @param fieldNameToFieldTypeMapping  the field name to {@link FieldType} mapping
     * @param requiresMultistepAggregation if aggregation is detected for the sql query
     * @param aliasHolder                  the {@link AliasHolder}
     */
    public WhereClauseProcessor(final FieldType defaultFieldType,
                                final Map<String, FieldType> fieldNameToFieldTypeMapping,
                                final boolean requiresMultistepAggregation, final AliasHolder aliasHolder) {
        this.defaultFieldType = defaultFieldType;
        this.fieldNameToFieldTypeMapping = fieldNameToFieldTypeMapping;
        this.requiresMultistepAggregation = requiresMultistepAggregation;
        this.aliasHolder = aliasHolder;
    }

    /**
     * Constructor without AliasHolder.
     *
     * @param defaultFieldType             the default {@link FieldType}
     * @param fieldNameToFieldTypeMapping  the field name to type mapping
     * @param requiresMultistepAggregation if multistep aggregation is required.
     */
    public WhereClauseProcessor(final FieldType defaultFieldType,
                                final Map<String, FieldType> fieldNameToFieldTypeMapping,
                                final boolean requiresMultistepAggregation) {
        this(defaultFieldType, fieldNameToFieldTypeMapping, requiresMultistepAggregation, new AliasHolder());
    }

    /**
     * Construtor to use when no value for requiresAggregation.
     *
     * @param defaultFieldType            the default {@link FieldType}
     * @param fieldNameToFieldTypeMapping the field name to {@link FieldType} map
     */
    public WhereClauseProcessor(final FieldType defaultFieldType,
                                final Map<String, FieldType> fieldNameToFieldTypeMapping) {
        this(defaultFieldType, fieldNameToFieldTypeMapping, false);
    }

    //Parse comparative expression != = < > => <= into mongo expr
    private void parseComparativeExpr(final Document query, final SQLExpr leftExpression,
                                      final SQLExpr rightExpression, final String comparatorType)
            throws ParseException {
        String operator = "$" + comparatorType;
        if (leftExpression instanceof SQLMethodInvokeExpr) {
            Document doc = new Document();
            Object leftParse = parseExpression(new Document(), leftExpression, rightExpression);
            Object rightParse = parseExpression(new Document(), rightExpression, leftExpression);
            doc.put(operator, Arrays.asList(leftParse, (SqlUtils.isColumn(rightExpression)
                    && !rightExpression.toString().startsWith("$") ? "$" + rightParse : rightParse)));
            if (requiresMultistepAggregation) {
                query.put("$expr", doc);
            } else {
                query.putAll(doc);
            }
        } else if (SqlUtils.isColumn(leftExpression) && SqlUtils.isColumn(rightExpression)) {
            if (requiresMultistepAggregation) {
                Document doc = new Document();
                String leftName = ((SQLPropertyExpr) leftExpression).getName();
                String rightName = ((SQLPropertyExpr) rightExpression).getName();
                doc.put(operator,
                        Arrays.asList((leftName.startsWith("$") ? leftName : "$" + leftName),
                                (rightName.startsWith("$") ? rightName : "$" + rightName)));
                query.put("$expr", doc);
            } else {
                query.put(parseExpression(new Document(), leftExpression, rightExpression).toString(),
                        parseExpression(new Document(), rightExpression, leftExpression));
            }
        } else if (rightExpression instanceof SQLMethodInvokeExpr) {
            Document doc = new Document();
            Object leftParse = parseExpression(new Document(), rightExpression, leftExpression);
            Object rightParse = parseExpression(new Document(), leftExpression, rightExpression);
            doc.put(operator, Arrays.asList(leftParse, (SqlUtils.isColumn(leftExpression)
                    && !leftExpression.toString().startsWith("$") ? "$" + rightParse : rightParse)));
            if (requiresMultistepAggregation) {
                query.put("$expr", doc);
            } else {
                query.putAll(doc);
            }
        } else if (SqlUtils.isColumn(leftExpression)) {
            Document subdocument = new Document();
            if (operator.equals("$eq")) {
                query.put(parseExpression(new Document(), leftExpression, rightExpression).toString(),
                        parseExpression(new Document(), rightExpression, leftExpression));
            } else {
                subdocument.put(operator, parseExpression(new Document(), rightExpression, leftExpression));
                query.put(parseExpression(new Document(), leftExpression, rightExpression).toString(),
                        subdocument);
            }
        } else {
            Document doc = new Document();
            Object leftParse = parseExpression(new Document(), leftExpression, rightExpression);
            doc.put(operator, Arrays.asList(leftParse, SqlUtils.nonFunctionToNode(
                    rightExpression, requiresMultistepAggregation)));
            if (requiresMultistepAggregation) {
                query.put("$expr", doc);
            } else {
                Document subdocument = new Document();
                if ("eq".equals(comparatorType) && leftParse instanceof String) {
                    query.put(leftParse.toString(), parseExpression(new Document(),
                            rightExpression, leftExpression));
                } else if (leftParse instanceof String) {
                    subdocument.put(operator, parseExpression(new Document(), rightExpression, leftExpression));
                    query.put(parseExpression(new Document(), leftExpression, rightExpression).toString(),
                            subdocument);
                } else {
                    query.putAll(doc);
                }
            }
        }
    }

    /**
     * Recursive function responsible for stepping through the sql structure and converting it into a mongo structure.
     *
     * @param query              the query in {@link Document} format
     * @param incomingExpression the incoming {@link SQLObject}
     * @param otherSide          the {@link SQLObject} on the other side
     * @return the converted mongo structure.
     * @throws ParseException if there is an issue parsing the incomingExpression
     */
    @SuppressWarnings("checkstyle:methodlength")
    public Object parseExpression(final Document query,
                                  final SQLObject incomingExpression, final SQLObject otherSide)
            throws ParseException {
        if (incomingExpression instanceof SQLBinaryOpExpr && ((SQLBinaryOpExpr) incomingExpression).getOperator() == SQLBinaryOperator.Like
                && ((SQLBinaryOpExpr) incomingExpression).getLeft() instanceof SQLName
                && (((SQLBinaryOpExpr) incomingExpression).getRight() instanceof SQLValuableExpr
                || ((SQLBinaryOpExpr) incomingExpression).getRight() instanceof SQLName)) {
            // like
            SQLBinaryOpExpr likeExpression = (SQLBinaryOpExpr) incomingExpression;
            String stringValueLeftSide = SqlUtils.getStringValue(likeExpression.getLeft());
            String stringValueRightSide = SqlUtils.getStringValue(likeExpression.getRight());
            String convertedRegexString = "^" + SqlUtils.replaceRegexCharacters(stringValueRightSide) + "$";
            Document document = new Document("$regex", convertedRegexString);
            document = new Document(stringValueLeftSide, document);
            query.putAll(document);
        } else if (incomingExpression instanceof SQLBinaryOpExpr && ((SQLBinaryOpExpr) incomingExpression).getOperator() == SQLBinaryOperator.NotLike
                && ((SQLBinaryOpExpr) incomingExpression).getLeft() instanceof SQLName
                && (((SQLBinaryOpExpr) incomingExpression).getRight() instanceof SQLValuableExpr
                || ((SQLBinaryOpExpr) incomingExpression).getRight() instanceof SQLName)) {
            // not like
            SQLBinaryOpExpr likeExpression = (SQLBinaryOpExpr) incomingExpression;
            String stringValueLeftSide = SqlUtils.getStringValue(likeExpression.getLeft());
            String stringValueRightSide = SqlUtils.getStringValue(likeExpression.getRight());
            String convertedRegexString = "^" + SqlUtils.replaceRegexCharacters(stringValueRightSide) + "$";
            Document document = new Document(stringValueLeftSide, new Document("$not", Pattern.compile(convertedRegexString)));
            query.putAll(document);
        } else if (incomingExpression instanceof SQLBinaryOpExpr) {
            RegexFunction regexFunction = SqlUtils.isRegexFunction(incomingExpression);
            DateFunction dateFunction = SqlUtils.getDateFunction(incomingExpression);
            ObjectIdFunction objectIdFunction = SqlUtils.isObjectIdFunction(this,
                    incomingExpression);
            SQLBinaryOpExpr sqlBinaryOpExpr = (SQLBinaryOpExpr) incomingExpression;
            SQLBinaryOperator operator = sqlBinaryOpExpr.getOperator();
            if (regexFunction != null) {
                Document regexDocument = new Document("$regex", regexFunction.getRegex());
                if (regexFunction.getOptions() != null) {
                    regexDocument.append("$options", regexFunction.getOptions());
                }
                query.put(regexFunction.getColumn(), wrapIfIsNot(regexDocument, regexFunction));
            } else if (dateFunction != null) {
                query.put(dateFunction.getColumn(),
                        new Document(dateFunction.getComparisonExpression(), dateFunction.getDate()));
            } else if (objectIdFunction != null) {
                query.put(objectIdFunction.getColumn(), objectIdFunction.toDocument());
            } else {
                SQLExpr left = sqlBinaryOpExpr.getLeft();
                SQLExpr right = sqlBinaryOpExpr.getRight();
                switch (operator) {
                    case BooleanAnd:
                        handleAndOr("$and", sqlBinaryOpExpr, query);
                        break;
                    case BooleanOr:
                        handleAndOr("$or", sqlBinaryOpExpr, query);
                        break;
                    case Equality:
                        parseComparativeExpr(query, left, right, "eq");
                        break;
                    case NotEqual:
                        parseComparativeExpr(query, left, right, "ne");
                        break;
                    case GreaterThan:
                        parseComparativeExpr(query, left, right, "gt");
                        break;
                    case LessThan:
                        parseComparativeExpr(query, left, right, "lt");
                        break;
                    case GreaterThanOrEqual:
                        parseComparativeExpr(query, left, right, "gte");
                        break;
                    case LessThanOrEqual:
                        parseComparativeExpr(query, left, right, "lte");
                        break;
                    case Is:
                        if (left instanceof SQLMethodInvokeExpr && right instanceof SQLNullExpr) {
                            Document result = ((Document) recurseFunctions(new Document(),
                                    left, defaultFieldType,
                                    fieldNameToFieldTypeMapping)).append("$exists", false);
                            query.putAll(result);
                        } else {
                            query.put(SqlUtils.getStringValue(left), new Document("$exists", false));
                        }
                        break;
                    case IsNot:
                        if (left instanceof SQLMethodInvokeExpr && right instanceof SQLNullExpr) {
                            Document result = ((Document) recurseFunctions(new Document(),
                                    left, defaultFieldType,
                                    fieldNameToFieldTypeMapping)).append("$exists", true);
                            query.putAll(result);
                        } else {
                            query.put(SqlUtils.getStringValue(left), new Document("$exists", true));
                        }
                        break;
                    default:
                }
            }
        } else if (incomingExpression instanceof SQLInListExpr) {
            // in ()
            final SQLInListExpr inExpression = (SQLInListExpr) incomingExpression;
            final SQLExpr leftExpression = inExpression.getExpr();
            final String leftExpressionAsString = SqlUtils.getStringValue(leftExpression);
            ObjectIdFunction objectIdFunction = SqlUtils.isObjectIdFunction(this, incomingExpression);

            if (objectIdFunction != null) {
                query.put(objectIdFunction.getColumn(), objectIdFunction.toDocument());
            } else {
                List<Object> objectList = inExpression.getTargetList().stream().map(sqlExpr -> {
                    try {
                        return parseExpression(new Document(), sqlExpr,
                                leftExpression);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());
                if (leftExpression instanceof SQLMethodInvokeExpr) {
                    String mongoInFunction = inExpression.isNot() ? "$fnin" : "$fin";
                    query.put(mongoInFunction, new Document("function", parseExpression(new Document(),
                            leftExpression, otherSide)).append("list", objectList));
                } else {
                    String mongoInFunction = inExpression.isNot() ? "$nin" : "$in";
                    Document doc = new Document();
                    if (requiresMultistepAggregation) {
                        List<Object> lobj = Arrays.asList(
                                SqlUtils.nonFunctionToNode(leftExpression, requiresMultistepAggregation), objectList);
                        doc.put(mongoInFunction, lobj);
                        query.put("$expr", doc);
                    } else {
                        doc.put(leftExpressionAsString, new Document().append(mongoInFunction, objectList));
                        query.putAll(doc);
                    }

                }
            }
        } else if (incomingExpression instanceof SQLBetweenExpr) {
            // between  and
            SQLBetweenExpr between = (SQLBetweenExpr) incomingExpression;
            SQLExpr testExpr = between.getTestExpr();
            SQLExpr beginExpr = between.getBeginExpr();
            SQLExpr endExpr = between.getEndExpr();
            SQLBinaryOpExpr start = new SQLBinaryOpExpr(testExpr, SQLBinaryOperator.GreaterThanOrEqual, beginExpr);

            SQLBinaryOpExpr end = new SQLBinaryOpExpr(testExpr, SQLBinaryOperator.LessThanOrEqual, endExpr);
            SQLBinaryOpExpr andExpression = new SQLBinaryOpExpr(between.isNot()
                    ? new SQLNotExpr(start) : start, SQLBinaryOperator.BooleanAnd, between.isNot() ? new SQLNotExpr(end) : end);
            return parseExpression(query, andExpression, otherSide);
        } else if (incomingExpression instanceof SQLSubqueryTableSource) {
            // "(" expression ")"
            SQLSubqueryTableSource parenthesis = (SQLSubqueryTableSource) incomingExpression;
            Object expression = parseExpression(new Document(), parenthesis, null);
            return expression;
        } else if (incomingExpression instanceof SQLNotExpr) {
            SQLNotExpr notExpression = (SQLNotExpr) incomingExpression;
            SQLExpr expression = notExpression.getExpr();
            if (expression instanceof SQLSubqueryTableSource) {
                return new Document("$nor", Arrays.asList(parseExpression(query, expression, otherSide)));
            } else if (expression instanceof SQLName) {
                return new Document(SqlUtils.getStringValue(expression), new Document("$ne", true));
            } else if (expression instanceof SQLBinaryOpExpr) {
                Document parsedDocument = (Document) parseExpression(query, expression, otherSide);
                String column = parsedDocument.keySet().iterator().next();
                Document value = parsedDocument.get(column, Document.class);
                return new Document(column, new Document("$not", value));
            }
        } else if (incomingExpression instanceof SQLMethodInvokeExpr) {
            SQLMethodInvokeExpr function = ((SQLMethodInvokeExpr) incomingExpression);
            RegexFunction regexFunction = SqlUtils.isRegexFunction(incomingExpression);
            ObjectIdFunction objectIdFunction = SqlUtils.isObjectIdFunction(this, incomingExpression);
            if (regexFunction != null) {
                Document regexDocument = new Document("$regex", regexFunction.getRegex());
                if (regexFunction.getOptions() != null) {
                    regexDocument.append("$options", regexFunction.getOptions());
                }
                query.put(regexFunction.getColumn(), wrapIfIsNot(regexDocument, regexFunction));
            } else if (objectIdFunction != null) {
                return objectIdFunction.toDocument();
            } else {
                return recurseFunctions(query, function, defaultFieldType, fieldNameToFieldTypeMapping);
            }
        } else if (otherSide == null) {
            return new Document(SqlUtils.getStringValue(incomingExpression), true);
        } else {
            return SqlUtils.getNormalizedValue(incomingExpression, otherSide,
                    defaultFieldType, fieldNameToFieldTypeMapping, aliasHolder, null);
        }
        return query;
    }

    private Object wrapIfIsNot(final Document regexDocument, final RegexFunction regexFunction) {
        if (regexFunction.isNot()) {
            if (regexFunction.getOptions() != null) {
                throw new IllegalArgumentException("$not regex not supported with options");
            }
            return new Document("$not", Pattern.compile(regexFunction.getRegex()));
        }
        return regexDocument;
    }

    /**
     * Recurse through functions in the sql structure to generate mongo query structure.
     *
     * @param query                       the query in {@link Document} format
     * @param object                      the value
     * @param defaultFieldType            the default {@link FieldType}
     * @param fieldNameToFieldTypeMapping the field name to{@link FieldType} map
     * @return the mongo structure
     * @throws ParseException if the value of the object param could not be parsed
     */
    protected Object recurseFunctions(final Document query, final Object object,
                                      final FieldType defaultFieldType,
                                      final Map<String, FieldType> fieldNameToFieldTypeMapping) throws ParseException {
        if (object instanceof SQLMethodInvokeExpr) {
            SQLMethodInvokeExpr function = (SQLMethodInvokeExpr) object;
            query.put("$" + SqlUtils.translateFunctionName(function.getMethodName()),
                    recurseFunctions(new Document(), function.getArguments(), defaultFieldType, fieldNameToFieldTypeMapping));
        } else if (object instanceof SQLValuesExpr) {
            SQLValuesExpr expressionList = (SQLValuesExpr) object;
            List<Object> objectList = new ArrayList<>();
            for (SQLExpr expression : expressionList.getValues()) {
                objectList.add(recurseFunctions(new Document(), expression,
                        defaultFieldType, fieldNameToFieldTypeMapping));
            }
            return objectList.size() == 1 ? objectList.get(0) : objectList;
        } else if (object instanceof SQLExpr) {
            return SqlUtils.getNormalizedValue((SQLExpr) object, null,
                    defaultFieldType, fieldNameToFieldTypeMapping, null);
        }

        return query.isEmpty() ? null : query;
    }

    private void handleAndOr(final String key, final SQLBinaryOpExpr incomingExpression,
                             final Document query) throws ParseException {
        final SQLExpr leftExpression = incomingExpression.getLeft();
        final SQLExpr rightExpression = incomingExpression.getRight();

        List result = flattenOrsOrAnds(new ArrayList(), leftExpression, leftExpression, rightExpression);

        if (result != null) {
            Collections.reverse(result);
            query.put(key, result);
        } else {
            query.put(key, Arrays.asList(parseExpression(new Document(), leftExpression, rightExpression),
                    parseExpression(new Document(), rightExpression, leftExpression)));
        }
    }

    private List flattenOrsOrAnds(final List arrayList, final SQLExpr firstExpression,
                                  final SQLExpr leftExpression,
                                  final SQLExpr rightExpression) throws ParseException {
        if (firstExpression.getClass().isInstance(leftExpression) && isOrAndExpression(leftExpression) && !isOrAndExpression(rightExpression)) {
            SQLExpr left = ((SQLBinaryOpExpr) leftExpression).getLeft();
            SQLExpr right = ((SQLBinaryOpExpr) leftExpression).getRight();
            arrayList.add(parseExpression(new Document(), rightExpression, null));
            List result = flattenOrsOrAnds(arrayList, firstExpression, left, right);
            if (result != null) {
                return arrayList;
            }
        } else if (isOrAndExpression(firstExpression)
                && !isOrAndExpression(leftExpression) && !isOrAndExpression(rightExpression)) {
            arrayList.add(parseExpression(new Document(), rightExpression, null));
            arrayList.add(parseExpression(new Document(), leftExpression, null));
            return arrayList;
        } else {
            return null;
        }
        return null;
    }

    private boolean isOrAndExpression(final SQLExpr expression) {
        if (expression instanceof SQLBinaryOpExpr) {
            SQLBinaryOperator operator = ((SQLBinaryOpExpr) expression).getOperator();
            return operator == SQLBinaryOperator.BooleanAnd || operator == SQLBinaryOperator.BooleanOr;
        }
        return false;
    }

}
