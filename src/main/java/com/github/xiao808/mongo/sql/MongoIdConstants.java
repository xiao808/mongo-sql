package com.github.xiao808.mongo.sql;

/**
 * mongo id、pagination constants
 *
 * @author zengxiao
 * @date 2023/5/5 17:52
 * @since 1.0
 **/
public interface MongoIdConstants {
    /**
     * sql field will be treated as mongo primary key
     */
    String REPRESENT_MONGO_ID = "id";

    /**
     * mongo object id
     */
    String MONGO_OBJECT_ID = "$oid";

    /**
     * mongo primary key field
     */
    String MONGO_ID = "_id";

    /**
     * pagination records field
     */
    String REPRESENT_PAGE_DATA = "data";

    /**
     * pagination records total number field
     */
    String REPRESENT_PAGE_TOTAL = "total";


    /**
     * dollar
     */
    String DOLLAR = "$";

    /**
     * dot
     */
    String DOT = ".";

    /**
     * underline, used for:
     * 1. replace with dot which in field with table alias
     * 2. placeholder just for Collection<Document>, cause mapping collection only support single Document. only SQLSelectQueryBlock has used placeholder currently.
     * additional:
     * Document is a Map struct, should not use placeholder to store Map, just using Document is enough.
     */
    String UNDERLINE = "_";

    /**
     * field with table alias will be replaced by underline
     */
    String REPLACED_BY_UNDERLINE = "\\.";

    /**
     * start with regex
     */
    String REGEX_START_WITH = "^";

    /**
     * empty string
     */
    String EMPTY_STRING = "";

    /**
     * sub query alias placeholder
     */
    String SUB_QUERY_ALIAS_PLACEHOLDER = "sub_alias";

    /**
     * sub query base alias placeholder
     */
    String SUB_QUERY_BASE_ALIAS_PLACEHOLDER = "_alias";

    /**
     * char will be removed at the start of field
     */
    String CHAR_WILL_BE_REMOVED_IN_FIELD_START = "`";

    /**
     * char will be removed at the end of field
     */
    String CHAR_WILL_BE_REMOVED_IN_FIELD_END = "`$";

    /**
     * used for on condition of join clause.
     */
    String LEFT_TABLE_ALIAS_OF_ON_CONDITION = "LEFT_TABLE_ALIAS_OF_ON_CONDITION";

    /**
     * used for on condition of join clause.
     */
    String ON_CONDITION = "ON_CONDITION";
}
