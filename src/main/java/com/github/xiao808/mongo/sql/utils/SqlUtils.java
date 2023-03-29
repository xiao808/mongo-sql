package com.github.xiao808.mongo.sql.utils;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLLimit;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.expr.SQLAllColumnExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.expr.SQLDateExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLInListExpr;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;
import com.alibaba.druid.sql.ast.expr.SQLNumericLiteralExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.expr.SQLTextLiteralExpr;
import com.alibaba.druid.sql.ast.expr.SQLTimestampExpr;
import com.alibaba.druid.sql.ast.expr.SQLValuableExpr;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.github.xiao808.mongo.sql.FieldType;
import com.github.xiao808.mongo.sql.ParseException;
import com.github.xiao808.mongo.sql.holder.AliasHolder;
import com.github.xiao808.mongo.sql.processor.WhereClauseProcessor;
import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;
import org.bson.Document;

import java.math.BigInteger;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public final class SqlUtils {
    private static final Pattern SURROUNDED_IN_QUOTES = Pattern.compile("^\"(.+)*\"$");
    private static final Pattern LIKE_RANGE_REGEX = Pattern.compile("(\\[.+?\\])");
    private static final String REGEXMATCH_FUNCTION = "regexMatch";
    private static final String NOT_REGEXMATCH_FUNCTION = "notRegexMatch";


    private static final DateTimeFormatter YY_MM_DDFORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter YYMMDDFORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter ISODateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");

    private static final Map<String, String> FUNCTION_MAPPER = new HashMap<>();

    static {
        FUNCTION_MAPPER.put("OID", "toObjectId");
        FUNCTION_MAPPER.put("TIMESTAMP", "toDate");
    }

    private static final Collection<DateTimeFormatter> FORMATTERS = Collections.unmodifiableList(Arrays.asList(
            ISODateTimeFormat,
            YY_MM_DDFORMATTER,
            YYMMDDFORMATTER));

    private static final Character NEGATIVE_NUMBER_SIGN = '-';

    private SqlUtils() {

    }

    /**
     * Will get the string value for an expression and remove double double quotes if they are present like,
     * i.e: ""45 days ago"".
     *
     * @param expression the {@link SQLObject}
     * @return the string value for an expression and remove double double quotes if they are present
     */
    public static String getStringValue(final SQLObject expression) {
        if (expression instanceof SQLValuableExpr) {
            return ((SQLValuableExpr) expression).getValue().toString();
        } else if (expression instanceof SQLName) {
            String columnName = ((SQLName) expression).getSimpleName();
            Matcher matcher = SURROUNDED_IN_QUOTES.matcher(columnName);
            if (matcher.matches()) {
                return matcher.group(1);
            }
            return columnName;
        }
        return expression.toString();
    }


    /**
     * Take an {@link SQLExpr} and normalize it.
     *
     * @param incomingExpression          the incoming expression
     * @param otherSide                   the other side of the expression
     * @param defaultFieldType            the default {@link FieldType}
     * @param fieldNameToFieldTypeMapping the field name to {@link FieldType} map
     * @param sign                        a negative or positive sign
     * @return the normalized value
     * @throws ParseException if there is a parsing issue
     */
    public static Object getNormalizedValue(final SQLExpr incomingExpression, final SQLExpr otherSide,
                                            final FieldType defaultFieldType,
                                            final Map<String, FieldType> fieldNameToFieldTypeMapping,
                                            final Character sign) throws ParseException {
        return getNormalizedValue(incomingExpression, otherSide, defaultFieldType, fieldNameToFieldTypeMapping,
                new AliasHolder(), sign);
    }


    /**
     * Take an {@link SQLExpr} and normalize it.
     *
     * @param incomingExpression          the incoming expression
     * @param otherSide                   the other side of the expression
     * @param defaultFieldType            the default {@link FieldType}
     * @param fieldNameToFieldTypeMapping the field name to {@link FieldType} map
     * @param aliasHolder                 an aliasHolder
     * @param sign                        a negative or positive sign
     * @return the normalized value
     * @throws ParseException if there is a parsing issue
     */
    public static Object getNormalizedValue(final SQLObject incomingExpression, final SQLObject otherSide,
                                            final FieldType defaultFieldType,
                                            final Map<String, FieldType> fieldNameToFieldTypeMapping,
                                            final AliasHolder aliasHolder,
                                            final Character sign)
            throws ParseException {
        FieldType fieldType = otherSide != null ? fieldNameToFieldTypeMapping.getOrDefault(getStringValue(otherSide), defaultFieldType) : FieldType.UNKNOWN;
        if (incomingExpression instanceof SQLNumericLiteralExpr) {
            return getNormalizedValue(convertToNegativeIfNeeded(((SQLNumericLiteralExpr) incomingExpression).getNumber(), sign),
                    fieldType);
        } else if (incomingExpression instanceof SQLTextLiteralExpr) {
            return getNormalizedValue((((SQLTextLiteralExpr) incomingExpression).getText()), fieldType);
        } else if (incomingExpression instanceof SQLIdentifierExpr) {
            Object normalizedColumn = getNormalizedValue(getStringValue(incomingExpression), fieldType);
            if (aliasHolder != null && !aliasHolder.isEmpty()
                    && normalizedColumn instanceof String
                    && aliasHolder.containsAliasForFieldExp((String) normalizedColumn)) {
                return aliasHolder.getAliasFromFieldExp((String) normalizedColumn);
            }
            return normalizedColumn;
        } else if (incomingExpression instanceof SQLTimestampExpr) {
            return getNormalizedValue(
                    new Date((((SQLTimestampExpr) incomingExpression).getDate(TimeZone.getDefault()).getTime())), fieldType);
        } else if (incomingExpression instanceof SQLDateExpr) {
            return getNormalizedValue((((SQLDateExpr) incomingExpression).getDate()), fieldType);
        } else {
            throw new ParseException("can not parseNaturalLanguageDate: " + incomingExpression.toString());
        }
    }

    private static Object convertToNegativeIfNeeded(final Number number, final Character sign) throws ParseException {
        if (NEGATIVE_NUMBER_SIGN.equals(sign)) {
            if (number instanceof Integer) {
                return -((Integer) number);
            } else if (number instanceof Long) {
                return -((Long) number);
            } else if (number instanceof Double) {
                return -((Double) number);
            } else if (number instanceof Float) {
                return -((Float) number);
            } else {
                throw new ParseException(String.format("could not convert %s into negative number", number));
            }
        } else {
            return number;
        }
    }

    /**
     * Take an {@link Object} and normalize it.
     *
     * @param value     the value to normalize
     * @param fieldType the {@link FieldType}
     * @return the normalized value
     * @throws ParseException if there is an issue parsing the query
     */
    public static Object getNormalizedValue(final Object value, final FieldType fieldType) throws ParseException {
        if (fieldType == null || FieldType.UNKNOWN.equals(fieldType)) {
            Object bool = getObjectAsBoolean(value);
            return (bool != null) ? bool : value;
        } else {
            if (FieldType.STRING.equals(fieldType)) {
                return fixDoubleSingleQuotes(forceString(value));
            }
            if (FieldType.NUMBER.equals(fieldType)) {
                return getObjectAsNumber(value);
            }
            if (FieldType.DATE.equals(fieldType)) {
                return getObjectAsDate(value);
            }
            if (FieldType.BOOLEAN.equals(fieldType)) {
                return Boolean.valueOf(value.toString());
            }
        }
        throw new ParseException("could not normalize value:" + value);
    }

    private static long getLongFromStringIfInteger(final String stringValue) throws ParseException {
        BigInteger bigInt = new BigInteger(stringValue);
        isFalse(bigInt.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0,
                stringValue + ": value is too large");
        return bigInt.longValue();
    }

    /**
     * get limit as long.
     *
     * @param limit the limit as a long
     * @return the limit
     * @throws ParseException if there is an issue parsing the query
     */
    public static long getLimitAsLong(final SQLLimit limit) throws ParseException {
        if (limit != null) {
            return getLongFromStringIfInteger(SqlUtils.getStringValue(limit.getRowCount()));
        }
        return -1;
    }

    /**
     * get offset as long.
     *
     * @param offset the offset
     * @return the offset
     */
    public static long getOffsetAsLong(final SQLLimit offset) {
        if (offset != null) {
            return Long.parseLong(((SQLValuableExpr) offset.getOffset()).getValue().toString());
        }
        return -1;
    }

    /**
     * Will replace double single quotes in regex with a single single quote,
     * i.e: "^[ae"don''tgaf]+$" -&gt; "^[ae"don'tgaf]+$".
     *
     * @param regex the regex
     * @return the regex without double single quotes
     */
    public static String fixDoubleSingleQuotes(final String regex) {
        return regex.replaceAll("''", "'");
    }

    /**
     * Will return tue if the query is select *.
     *
     * @param selectItems list of {@link SQLSelectItem}s
     * @return true if select *
     */
    public static boolean isSelectAll(final List<SQLSelectItem> selectItems) {
        if (selectItems != null && selectItems.size() == 1) {
            SQLSelectItem firstItem = selectItems.get(0);
            return firstItem.getExpr() instanceof SQLAllColumnExpr;
        } else {
            return false;
        }
    }

    /**
     * Will return true if query is doing a count(*).
     *
     * @param selectItems list of {@link SQLSelectItem}s
     * @return true if query is doing a count(*)
     */
    public static boolean isCountAll(final List<SQLSelectItem> selectItems) {
        if (selectItems != null && selectItems.size() == 1) {
            SQLSelectItem firstItem = selectItems.get(0);
            if (firstItem.getExpr() instanceof SQLMethodInvokeExpr) {
                SQLMethodInvokeExpr function = (SQLMethodInvokeExpr) (firstItem).getExpr();
                if ("count(*)".equals(function.toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Convert object to boolean.
     *
     * @param value the value to convert to boolean
     * @return boolean value of object
     */
    public static Object getObjectAsBoolean(final Object value) {
        if (value.toString().equalsIgnoreCase("true")
                || value.toString().equalsIgnoreCase("false")) {
            return Boolean.valueOf(value.toString());
        }
        return null;
    }

    /**
     * Convert object to {@link Date} object.
     *
     * @param value the object to convert to a {@link Date}
     * @return date object
     * @throws ParseException if there is an issue parsing the query
     */
    public static Object getObjectAsDate(final Object value) throws ParseException {
        if (value instanceof String) {
            for (DateTimeFormatter formatter : FORMATTERS) {
                try {
                    TemporalAccessor accessor = formatter.parse((String) value);
                    return Date.from(Instant.from(accessor));
                } catch (Exception e) {
                    //noop
                }
            }
            try {
                return parseNaturalLanguageDate((String) value);
            } catch (Exception e) {
                //noop
            }

        }
        throw new ParseException("could not convert " + value + " to a date");
    }

    /**
     * Parse natural language to Date object.
     *
     * @param text the natural language text to convert to a date
     * @return parsed date
     */
    public static Date parseNaturalLanguageDate(final String text) {
        Parser parser = new Parser();
        List<DateGroup> groups = parser.parse(text);
        for (DateGroup group : groups) {
            List<Date> dates = group.getDates();
            if (dates.size() > 0) {
                return dates.get(0);
            }
        }
        throw new IllegalArgumentException("could not natural language date: " + text);
    }

    /**
     * Get object as number.
     *
     * @param value the object to convert to a number
     * @return number
     * @throws ParseException if there is an issue parsing the query
     */
    public static Object getObjectAsNumber(final Object value) throws ParseException {
        if (String.class.isInstance(value)) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e1) {
                try {
                    return Double.parseDouble((String) value);
                } catch (NumberFormatException e2) {
                    try {
                        return Float.parseFloat((String) value);
                    } catch (NumberFormatException e3) {
                        throw new ParseException("could not convert " + value + " to number", e3);
                    }
                }
            }
        } else {
            return value;
        }
    }

    /**
     * Force an object to being a string.
     *
     * @param value the object to try to force to a string
     * @return the object converted to a string
     */
    public static String forceString(final Object value) {
        if (String.class.isInstance(value)) {
            return (String) value;
        } else {
            return "" + value + "";
        }
    }

    /**
     * Will replace a LIKE sql query with the format for a regex.
     *
     * @param value the regex
     * @return a regex that represents the LIKE format.
     */
    public static String replaceRegexCharacters(final String value) {
        String newValue = value.replaceAll("%", ".*")
                .replaceAll("_", ".{1}");

        Matcher m = LIKE_RANGE_REGEX.matcher(newValue);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, m.group(1) + "{1}");
        }
        m.appendTail(sb);

        return sb.toString();
    }

    /**
     * Get the columns used for the group by from the {@link SQLSelectQueryBlock}.
     *
     * @param plainSelect the {@link SQLSelectQueryBlock} object
     * @return the columns used for the group by from the {@link SQLSelectQueryBlock}.
     */
    public static List<String> getGroupByColumnReferences(final SQLSelectQueryBlock plainSelect) {
        if (plainSelect.getGroupBy() == null) {
            return Collections.emptyList();
        }
        return plainSelect.getGroupBy().getItems().stream().map(SqlUtils::getStringValue).collect(Collectors.toList());
    }

    /**
     * Will take the expression and create an {@link ObjectIdFunction} if the expression is an ObjectId function.
     * Otherwise it will return null.
     *
     * @param whereClauseProcessor the {@link SQLObject}
     * @param incomingExpression   the incoming expression
     * @return the {@link ObjectIdFunction}
     * @throws ParseException if there is an issue parsing the query
     */
    public static ObjectIdFunction isObjectIdFunction(final WhereClauseProcessor whereClauseProcessor,
                                                      final SQLObject incomingExpression) throws ParseException {
        if (incomingExpression instanceof SQLBinaryOpExpr) {
            SQLBinaryOpExpr comparisonOperator = (SQLBinaryOpExpr) incomingExpression;
            String rightExpression = getStringValue(comparisonOperator.getRight());
            if (comparisonOperator.getLeft() instanceof SQLMethodInvokeExpr) {
                SQLMethodInvokeExpr function = ((SQLMethodInvokeExpr) comparisonOperator.getLeft());
                if ("toobjectid".equalsIgnoreCase(function.getMethodName())
                        && (function.getArguments().size() == 1)
                        && function.getArguments().get(0) instanceof SQLValuableExpr) {
                    String column = getStringValue(function.getArguments().get(0));
                    return new ObjectIdFunction(column, rightExpression, comparisonOperator);
                } else if ("objectid".equalsIgnoreCase(function.getMethodName())
                        && (function.getArguments().size() == 1)
                        && function.getArguments().get(0) instanceof SQLValuableExpr) {
                    String column = getStringValue(function.getArguments().get(0));
                    return new ObjectIdFunction(column, rightExpression, comparisonOperator);
                }
            } else if (comparisonOperator.getRight() instanceof SQLMethodInvokeExpr) {
                SQLMethodInvokeExpr function = ((SQLMethodInvokeExpr) comparisonOperator.getRight());
                if ("toobjectid".equalsIgnoreCase(translateFunctionName(function.getMethodName()))
                        && (function.getArguments().size() == 1)
                        && function.getArguments().get(0) instanceof SQLValuableExpr) {
                    String column = getStringValue(comparisonOperator.getLeft());
                    return new ObjectIdFunction(column, getStringValue(function.getArguments().get(0)), comparisonOperator);
                }
            }
        } else if (incomingExpression instanceof SQLInListExpr) {
            SQLInListExpr inExpression = (SQLInListExpr) incomingExpression;
            final SQLExpr leftExpression = ((SQLInListExpr) incomingExpression).getExpr();

            if (inExpression.getExpr() instanceof SQLMethodInvokeExpr) {
                SQLMethodInvokeExpr function = ((SQLMethodInvokeExpr) inExpression.getExpr());
                if ("objectid".equalsIgnoreCase(function.getMethodName())
                        && (function.getArguments().size() == 1)
                        && function.getArguments().get(0) instanceof SQLValuableExpr) {
                    String column = getStringValue(function.getArguments().get(0));
                    List<Object> rightExpression = inExpression.getTargetList().stream().map(sqlExpr -> {
                        try {
                            return whereClauseProcessor.parseExpression(new Document(), sqlExpr, leftExpression);
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    }).collect(Collectors.toList());
                    return new ObjectIdFunction(column, rightExpression, inExpression);
                }
            }
        } else if (incomingExpression instanceof SQLMethodInvokeExpr) {
            SQLMethodInvokeExpr function = ((SQLMethodInvokeExpr) incomingExpression);
            if ("toobjectid".equalsIgnoreCase(translateFunctionName(function.getMethodName())) && (function.getArguments().size() == 1) && function.getArguments().get(0) instanceof SQLValuableExpr) {
                return new ObjectIdFunction(null, getStringValue(function.getArguments().get(0)), new SQLBinaryOpExpr(null, SQLBinaryOperator.Equality, function.getArguments().get(0)));
            }
        }
        return null;
    }

    /**
     * return a {@link DateFunction} if this {@link SQLObject} is a date.
     *
     * @param incomingExpression the {@link SQLObject} object
     * @return the {@link DateFunction} or null if not a date.
     * @throws ParseException if there is an issue parsing the query
     */
    public static DateFunction getDateFunction(final SQLObject incomingExpression) throws ParseException {
        if (incomingExpression instanceof SQLBinaryOpExpr) {
            SQLBinaryOpExpr comparisonOperator = (SQLBinaryOpExpr) incomingExpression;
            String rightExpression = getStringValue(comparisonOperator.getRight());
            if (comparisonOperator.getLeft() instanceof SQLMethodInvokeExpr) {
                SQLMethodInvokeExpr function = ((SQLMethodInvokeExpr) comparisonOperator.getLeft());
                if ("date".equalsIgnoreCase(function.getMethodName())
                        && (function.getArguments().size() == 2)
                        && function.getArguments().get(1) instanceof SQLValuableExpr) {
                    String column = getStringValue(function.getArguments().get(0));
                    try {
                        return new DateFunction(
                                getStringValue(function.getArguments().get(1)),
                                rightExpression, column, comparisonOperator);
                    } catch (IllegalArgumentException e) {
                        throw new ParseException(e);
                    }
                }

            }
        }
        return null;
    }

    /**
     * return a {@link RegexFunction} if this {@link SQLObject} is a regex.
     *
     * @param incomingExpression the {@link SQLObject} object
     * @return the {@link RegexFunction} or null if not a regex.
     * @throws ParseException if there is an issue parsing the query
     */
    @SuppressWarnings("checkstyle:magicnumber")
    public static RegexFunction isRegexFunction(final SQLObject incomingExpression) throws ParseException {
        if (incomingExpression instanceof SQLBinaryOpExpr) {
            SQLBinaryOpExpr equalsTo = (SQLBinaryOpExpr) incomingExpression;
            String rightExpression = equalsTo.getRight().toString();
            if (equalsTo.getLeft() instanceof SQLMethodInvokeExpr) {
                SQLMethodInvokeExpr function = ((SQLMethodInvokeExpr) equalsTo.getLeft());
                if ((REGEXMATCH_FUNCTION.equalsIgnoreCase(function.getMethodName())
                        || NOT_REGEXMATCH_FUNCTION.equalsIgnoreCase(function.getMethodName()))
                        && (function.getArguments().size() == 2
                        || function.getArguments().size() == 3)
                        && function.getArguments().get(1) instanceof SQLValuableExpr) {

                    final Boolean rightExpressionValue = Boolean.valueOf(rightExpression);

                    isTrue(rightExpressionValue, "false is not allowed for regexMatch function");

                    RegexFunction regexFunction = getRegexFunction(function,
                            NOT_REGEXMATCH_FUNCTION.equalsIgnoreCase(function.getMethodName()));
                    return regexFunction;
                }

            }
        } else if (incomingExpression instanceof SQLMethodInvokeExpr) {
            SQLMethodInvokeExpr function = ((SQLMethodInvokeExpr) incomingExpression);
            if ((REGEXMATCH_FUNCTION.equalsIgnoreCase(function.getMethodName())
                    || NOT_REGEXMATCH_FUNCTION.equalsIgnoreCase(function.getMethodName()))
                    && (function.getArguments().size() == 2
                    || function.getArguments().size() == 3)
                    && function.getArguments().get(1) instanceof SQLValuableExpr) {

                RegexFunction regexFunction = getRegexFunction(function,
                        NOT_REGEXMATCH_FUNCTION.equalsIgnoreCase(function.getMethodName()));
                return regexFunction;
            }
        }
        return null;
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private static RegexFunction getRegexFunction(final SQLMethodInvokeExpr function,
                                                  final boolean isNot) throws ParseException {
        final String column = getStringValue(function.getArguments().get(0));
        final String regex = fixDoubleSingleQuotes(getStringValue(function.getArguments().get(1)));
        try {
            Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            throw new ParseException(e);
        }
        RegexFunction regexFunction = new RegexFunction(column, regex, isNot);

        if (function.getArguments().size() == 3 && function.getArguments().get(2) instanceof SQLValuableExpr) {
            regexFunction.setOptions(getStringValue(function.getArguments().get(2)));
        }
        return regexFunction;
    }

    /**
     * <p>Validate that the argument condition is <code>true</code>; otherwise
     * throwing an exception with the specified message. This method is useful when
     * validating according to an arbitrary boolean expression, such as validating an
     * object or using your own custom validation expression.</p>
     *
     * <pre>SqlUtils.isTrue( myObject.isOk(), "The object is not OK: ");</pre>
     *
     * <p>For performance reasons, the object value is passed as a separate parameter and
     * appended to the exception message only in the case of an error.</p>
     *
     * @param expression the boolean expression to check
     * @param message    the exception message if invalid
     * @throws ParseException if expression is <code>false</code>
     */
    public static void isTrue(final boolean expression, final String message) throws ParseException {
        if (!expression) {
            throw new ParseException(message);
        }
    }

    /**
     * <p>Validate that the argument condition is <code>false</code>; otherwise
     * throwing an exception with the specified message. This method is useful when
     * validating according to an arbitrary boolean expression, such as validating an
     * object or using your own custom validation expression.</p>
     *
     * <pre>SqlUtils.isFalse( myObject.isOk(), "The object is not OK: ");</pre>
     *
     * <p>For performance reasons, the object value is passed as a separate parameter and
     * appended to the exception message only in the case of an error.</p>
     *
     * @param expression the boolean expression to check
     * @param message    the exception message if invalid
     * @throws ParseException if expression is <code>false</code>
     */
    public static void isFalse(final Boolean expression, final String message) throws ParseException {
        if (expression) {
            throw new ParseException(message);
        }
    }

    /**
     * Is the expression a column.
     *
     * @param expression the {@link SQLExpr}
     * @return true if is a column.
     */
    public static boolean isColumn(final SQLExpr expression) {
        if (expression instanceof SQLName) {
            return !((SQLName) expression).getSimpleName().matches("^(\".*\"|true|false)$");
        }
        return false;
    }

    /**
     * Remove tablename from column.  For instance will rename column from c.column1 to column1.
     *
     * @param column    the column
     * @param aliasBase the alias base
     * @return the column without the tablename
     */
    public static SQLPropertyExpr removeAliasFromColumn(final SQLPropertyExpr column, final String aliasBase) {
        if (column.matchOwner(aliasBase)) {
            column.setOwner("");
        }
        return column;
    }


    /**
     * Remove tablename from column.  For instance will rename column from c.column1 to column1.
     *
     * @param selectExpressionItem the {@link SQLSelectItem}
     * @param aliasBase            the alias base
     * @return the column without the tablename in the {@link SQLSelectItem}
     */
    public static SQLSelectItem removeAliasFromSelectExpressionItem(final SQLSelectItem selectExpressionItem, final String aliasBase) {
        if (selectExpressionItem != null && selectExpressionItem.getExpr() instanceof SQLPropertyExpr) {
            removeAliasFromColumn((SQLPropertyExpr) selectExpressionItem.getExpr(), aliasBase);
        }
        return selectExpressionItem;
    }

    /**
     * For nested fields we need it, no alias, clear first part "t1."column1.nested1, alias is mandatory.
     *
     * @param column the column object
     * @return the nested fields without the first part
     */
    public static String getColumnNameFromColumn(final SQLExpr column) {
        if (column instanceof SQLPropertyExpr) {
            String[] splitedNestedField = ((SQLPropertyExpr)column).getName().split("\\.");
            if (splitedNestedField.length > 2) {
                return String.join(".", Arrays.copyOfRange(splitedNestedField, 1, splitedNestedField.length));
            } else {
                return splitedNestedField[splitedNestedField.length - 1];
            }
        }
        return "";
    }

    /**
     * Will return true if the alias is an alias referenced in the column.  For instance, i.e: tableAlias r is a
     * table alias in the column r.cuisine.
     *
     * @param column     the column
     * @param tableAlias the table alias
     * @return true if the alias is an alias referenced in the column.
     */
    public static boolean isTableAliasOfColumn(final SQLPropertyExpr column, final String tableAlias) {
        return column.getOwnerName().equalsIgnoreCase(tableAlias);
    }

    /**
     * Is this expression one that would justify aggregation, like: max().
     *
     * @param field the field
     * @return true if the expession would justify aggregation.
     */
    public static boolean isAggregateExpression(final SQLExpr field) {
        if (field instanceof SQLMethodInvokeExpr) {
            SQLMethodInvokeExpr function = ((SQLMethodInvokeExpr) field);
            String fieldForAgg = function.getMethodName().trim().toLowerCase();
            return fieldForAgg.startsWith("sum(") || fieldForAgg.startsWith("avg(")
                    || fieldForAgg.startsWith("min(") || fieldForAgg.startsWith("max(")
                    || fieldForAgg.startsWith("count(");
        }
        return false;
    }

    /**
     * Get the name for field to be used in aggregation based on the function name and the alias.
     *
     * @param function the function name
     * @param alias    the alias
     * @return the field name to the alias mapping.
     * @throws ParseException if there is an issue parsing the query
     */
    public static Map.Entry<String, String> generateAggField(final SQLMethodInvokeExpr function,
                                                             final String alias) throws ParseException {
        String field = getFieldFromFunction(function);
        String functionName = function.getMethodName().toLowerCase();
        if ("*".equals(field) || functionName.equals("count")) {
            return new AbstractMap.SimpleEntry<>(field, (alias == null ? functionName : alias));
        } else {
            return new AbstractMap.SimpleEntry<>(field, (alias == null
                    ? functionName + "_" + field.replaceAll("\\.", "_") : alias));
        }

    }

    /**
     * Get field name from the function, i.e: MAX(advance_amount) -&gt; advance_amount.
     *
     * @param function the {@link SQLMethodInvokeExpr} object
     * @return the field name from the function.
     * @throws ParseException if there is an issue parsing the query
     */
    public static String getFieldFromFunction(final SQLMethodInvokeExpr function) throws ParseException {
        if (function.getArguments() != null && function.getArguments().size() == 1 && function.getArguments().get(0) instanceof SQLAllColumnExpr) {
            return null;
        }
        List<String> parameters = function.getArguments() == null
                ? Collections.<String>emptyList() : function.getArguments().stream().map(SqlUtils::getStringValue).collect(Collectors.toList());
        if (parameters.size() > 1) {
            throw new ParseException(function.getMethodName() + " function can only have one parameter");
        }
        return parameters.size() > 0 ? parameters.get(0) : null;
    }

    /**
     * Will prepend $ to the expression if it is a column.
     *
     * @param exp                          the expression
     * @param requiresMultistepAggregation if requires aggregation
     * @return string with prepended $ to the expression if it is a column.
     * @throws ParseException if there is an issue parsing the query
     */
    public static Object nonFunctionToNode(final SQLExpr exp, final boolean requiresMultistepAggregation)
            throws ParseException {
        return (SqlUtils.isColumn(exp) && !((SQLName) exp).getSimpleName().startsWith("$") && requiresMultistepAggregation)
                ? ("$" + exp) : getNormalizedValue(exp, null, FieldType.UNKNOWN, null, null);
    }

    /**
     * Will return true if any of the {@link SQLSelectItem}s has a function that justifies aggregation like max().
     *
     * @param selectItems list of {@link SQLSelectItem}s
     * @return true if any of the {@link SQLSelectItem}s has a function that justifies aggregation like max()
     */
    public static boolean isTotalGroup(final List<SQLSelectItem> selectItems) {
        for (SQLSelectItem sitem : selectItems) {
            if (isAggregateExpression(sitem.getExpr())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Clone an {@link SQLExpr}.
     *
     * @param expression the expression
     * @return the clone of an expression
     */
    public static SQLExpr cloneExpression(final SQLExpr expression) {
        if (expression == null) {
            return null;
        }
        try {
            return expression.clone();
        } catch (Exception e) {
            // Never exception because clone
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Will translate function name for speciality function names.
     *
     * @param functionName the function name to translate
     * @return the translated function name.
     */
    public static String translateFunctionName(final String functionName) {
        String transfunction = FUNCTION_MAPPER.get(functionName);
        if (transfunction != null) {
            return transfunction;
        } else {
            return functionName;
        }

    }
}
