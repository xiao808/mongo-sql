package com.github.xiao808.mongo.sql;

/**
 * mongo lexer constants
 *
 * @author zengxiao
 * @date 2023/5/5 17:10
 * @since 1.0
 **/
public interface LexerConstants {
    /* update lexer */
    /**
     * update column to target value
     */
    String SET = "$set";
    /**
     * update column to null
     */
    String UNSET = "$unset";

    /* comparator lexer */
    /**
     * in
     */
    String IN = "$in";
    /**
     * not in
     */
    String NIN = "$nin";
    /**
     * grater than or equal
     */
    String GTE = "$gte";
    /**
     * grater than
     */
    String GT = "$gt";
    /**
     * less than or equal
     */
    String LTE = "$lte";
    /**
     * less than
     */
    String LT = "$lt";
    /**
     * equal
     */
    String EQ = "$eq";
    /**
     * and
     */
    String AND = "$and";
    /**
     * or
     */
    String OR = "$or";
    /**
     * not
     */
    String NOT = "$not";
    /**
     * against
     */
    String NOR = "$nor";
    /**
     * not equal
     */
    String NE = "$ne";
    /**
     * is null
     */
    String IS_NULL = "$$ifNull";
    /**
     * exists
     */
    String EXISTS = "$exists";
    /**
     * like
     */
    String REGEX = "$regex";

    /**
     * like
     */
    String REGEX_MATCH = "$regexMatch";

    /**
     * input
     */
    String INPUT = "input";

    /**
     * regex without dollar
     */
    String REGEX_ORIGIN = "regex";

    /**
     * to string
     */
    String TO_STRING = "$toString";

    /* condition lexer */
    /**
     * where、on
     */
    String MATCH = "$match";
    /**
     * group by
     */
    String GROUP = "$group";

    /* table alias lexer */
    /**
     * pipeline, temporary table
     */
    String UNWIND = "$unwind";

    /* join lexer */
    /**
     * join
     */
    String LOOKUP = "$lookup";

    /* pagination lexer */
    /**
     * limit
     */
    String LIMIT = "$limit";
    /**
     * offset
     */
    String SKIP = "$skip";

    /* sort lexer */
    /**
     * sort
     */
    String SORT = "$sort";

    /* transform lexer */
    /**
     * replace $$ROOT
     */
    String REPLACE_ROOT = "$replaceRoot";
    /**
     * new root, change $$ROOT
     */
    String NEW_ROOT = "newRoot";
    /**
     * combine tables
     */
    String MERGE_OBJECTS = "$mergeObjects";
    /**
     * split array to single element
     */
    String ARRAY_ELEM_AT = "$arrayElemAt";
    /**
     * as
     */
    String PROJECT = "$project";
    /**
     * split pipeline
     */
    String FACET = "$facet";
    /**
     * pipeline counter
     */
    String TOTAL = "$total";

    /* base table lexer */
    /**
     * root table
     */
    String ROOT = "$$ROOT";

    /* regex lexer */
    /**
     * expr
     */
    String EXPR = "$expr";

    /* function lexer */
    /**
     * avg
     */
    String AVG = "$avg";
    /**
     * min
     */
    String MIN = "$min";
    /**
     * max
     */
    String MAX = "$max";
    /**
     * sum
     */
    String SUM = "$sum";
    /**
     * count
     */
    String COUNT = "$count";
    /**
     * trim
     */
    String TRIM = "$trim";
    /**
     * concat
     */
    String CONCAT = "$concat";
    /**
     * abs
     */
    String ABS = "$abs";
    /**
     * add
     */
    String ADD = "$add";
    /**
     * ceil
     */
    String CEIL = "$ceil";
    /**
     * divide
     */
    String DIVIDE = "$divide";
    /**
     * mod
     */
    String MOD = "$mod";
    /**
     * pow
     */
    String POW = "$pow";
    /**
     * convert date to string
     */
    String DATE_TO_STRING = "$dateToString";
    /**
     * argument of date to string
     */
    String DATE = "date";
    /**
     * argument of date to string
     */
    String FORMAT = "format";
    /**
     * year
     */
    String YEAR = "$year";
    /**
     * month
     */
    String MONTH = "$month";
    /**
     * day of month
     */
    String DAY_OF_MONTH = "$dayOfMonth";
    /**
     * hour
     */
    String HOUR = "$hour";
    /**
     * minute
     */
    String MINUTE = "$minute";
    /**
     * second
     */
    String SECOND = "$second";

    /* lookup lexer */
    /**
     * from table name
     */
    String FROM = "from";
    /**
     * set a = xxx;
     */
    String LET = "let";
    /**
     * pipeline
     */
    String PIPELINE = "pipeline";
    /**
     * table/field alias
     */
    String AS = "as";
    /**
     * alias path
     */
    String PATH = "path";
    /**
     * preserve null and empty arrays, used in join result
     */
    String PRESERVE_NULL_AND_EMPTY_ARRAYS = "preserveNullAndEmptyArrays";

    /**
     * all column
     */
    String ALL = "*";
    /**
     * join in 3.2
     */
    String LOCAL_FIELD = "localField";
    /**
     * join in 3.2
     */
    String FOREIGN_FIELD = "foreignField";
}
