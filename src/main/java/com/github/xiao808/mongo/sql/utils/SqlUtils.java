package com.github.xiao808.mongo.sql.utils;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.expr.SQLAggregateExpr;
import com.alibaba.druid.sql.ast.expr.SQLAllColumnExpr;
import com.alibaba.druid.sql.ast.expr.SQLDateExpr;
import com.alibaba.druid.sql.ast.expr.SQLNumericLiteralExpr;
import com.alibaba.druid.sql.ast.expr.SQLTextLiteralExpr;
import com.alibaba.druid.sql.ast.expr.SQLTimestampExpr;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.github.xiao808.mongo.sql.AggregateEnum;

import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.xiao808.mongo.sql.MongoIdConstants.UNDERLINE;

/**
 * utils for sql parsing.
 *
 * @author zengxiao
 * @date 2023-05-06 16:13
 * @since 1.0
 */
public final class SqlUtils {

    /**
     * for regex
     */
    private static final Pattern LIKE_RANGE_REGEX = Pattern.compile("(\\[.+?])");

    /**
     * private constructor
     */
    private SqlUtils() {
    }


    /**
     * if the query is select *.
     *
     * @param selectItems sql select item list
     * @return true if select *
     */
    public static boolean isSelectAll(final List<SQLSelectItem> selectItems) {
        return selectItems != null && selectItems.size() == 1 && selectItems.get(0).getExpr() instanceof SQLAllColumnExpr;
    }

    /**
     * if query is doing a count(*).
     *
     * @param selectItems sql select item list
     * @return true if query is count(*)
     */
    public static boolean isCountAll(final List<SQLSelectItem> selectItems) {
        if (selectItems != null && selectItems.size() == 1) {
            SQLExpr expr = selectItems.get(0).getExpr();
            List<SQLExpr> arguments;
            if (expr instanceof SQLAggregateExpr && (arguments = ((SQLAggregateExpr) expr).getArguments()).size() == 1) {
                return AggregateEnum.COUNT.getName().equalsIgnoreCase(((SQLAggregateExpr) expr).getMethodName()) && arguments.get(0) instanceof SQLAllColumnExpr;
            }
        }
        return false;
    }

    /**
     * if any of the sql select item is sql aggregate function
     *
     * @param selectItems sql select item list
     * @return true if any of the sql select item is sql aggregate function
     */
    public static boolean hasAggregateOnSelectItemList(final List<SQLSelectItem> selectItems) {
        return selectItems.stream().anyMatch(sqlSelectItem -> sqlSelectItem.getExpr() instanceof SQLAggregateExpr);
    }

    /**
     * construct regex for like clause
     *
     * @param value the regex
     * @return regex for like clause.
     */
    public static String constructLikeRegex(final String value) {
        String newValue = value.replaceAll("%", ".*").replaceAll(UNDERLINE, ".{1}");
        Matcher m = LIKE_RANGE_REGEX.matcher(newValue);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, m.group(1) + "{1}");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * get column name without table alias
     *
     * @param column sql expr
     * @return column name while expr is instance of SqlName
     */
    public static String getColumnNameWithOutTableAlias(final SQLExpr column) {
        if (column instanceof SQLName) {
            return ((SQLName) column).getSimpleName();
        }
        return "";
    }

    /**
     * get real value
     *
     * @param sqlObject sql object
     * @return the real value
     */
    public static Object getRealValue(final SQLObject sqlObject) {
        if (sqlObject instanceof SQLNumericLiteralExpr) {
            return ((SQLNumericLiteralExpr) sqlObject).getNumber();
        } else if (sqlObject instanceof SQLTextLiteralExpr) {
            return ((SQLTextLiteralExpr) sqlObject).getText();
        } else if (sqlObject instanceof SQLName) {
            return sqlObject.toString();
        } else if (sqlObject instanceof SQLTimestampExpr) {
            return ((SQLTimestampExpr) sqlObject).getDate(TimeZone.getDefault()).getTime();
        } else if (sqlObject instanceof SQLDateExpr) {
            return ((SQLDateExpr) sqlObject).getDate();
        }
        return null;
    }
}
