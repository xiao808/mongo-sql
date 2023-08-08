package com.github.xiao808.mongo.sql.visitor;

import com.alibaba.druid.util.StringUtils;

/**
 * sql visitor factory
 *
 * @author zengxiao
 * @date 2023/8/7 16:10
 * @since 1.0
 **/
public class SqlVisitorFactory {

    /**
     * mongodb version 3.2
     */
    private final static String VERSION_3_2 = "3.2";

    /**
     * get sql visitor according to version of mongodb.
     *
     * @param version version of mongodb
     * @return sql visitor
     */
    public static SqlVisitor create(String version) {
        if (!StringUtils.isEmpty(version) && version.startsWith(VERSION_3_2)) {
            return new SqlStatementTransformVisitorV32();
        }
        return new SqlStatementTransformVisitor();
    }
}
