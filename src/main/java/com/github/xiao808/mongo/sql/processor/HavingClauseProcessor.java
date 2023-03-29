package com.github.xiao808.mongo.sql.processor;

import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;
import com.github.xiao808.mongo.sql.FieldType;
import com.github.xiao808.mongo.sql.ParseException;
import com.github.xiao808.mongo.sql.holder.AliasHolder;
import com.github.xiao808.mongo.sql.utils.SqlUtils;
import org.bson.Document;

import java.util.Map;

/**
 * Processor for handling sql having clause.
 */
public class HavingClauseProcessor extends WhereClauseProcessor {



    /**
     * Default Constructor.
     * @param defaultFieldType the default {@link FieldType}
     * @param fieldNameToFieldTypeMapping the field name to {@link FieldType} mapping
     * @param aliasHolder the {@link AliasHolder}
     * @param requiresAggregation true if this sql statement requires aggregation
     */
    public HavingClauseProcessor(final FieldType defaultFieldType,
                                 final Map<String, FieldType> fieldNameToFieldTypeMapping,
                                 final AliasHolder aliasHolder, final boolean requiresAggregation) {
        super(defaultFieldType, fieldNameToFieldTypeMapping, requiresAggregation, aliasHolder);
    }

    /**
     * Recurse through functions in the sql structure to generate mongo query structure.
     * @param query the query object
     * @param object the value that needs to be parsed
     * @param defaultFieldType the default {@link FieldType}
     * @param fieldNameToFieldTypeMapping the field name to {@link FieldType}
     * @return the mongo structure
     * @throws ParseException if the value cannot be parsed
     */
    @Override //for use alias because it's after group expression so alias is already applied
    protected Object recurseFunctions(final Document query,
                                      final Object object,
                                      final FieldType defaultFieldType,
                                      final Map<String, FieldType> fieldNameToFieldTypeMapping)
            throws ParseException {
        if (object instanceof SQLMethodInvokeExpr) {
            SQLMethodInvokeExpr function = (SQLMethodInvokeExpr) object;
            if (SqlUtils.isAggregateExpression(function)) {
                String alias = aliasHolder.getAliasFromFieldExp(function.toString());
                return "$" + SqlUtils.generateAggField(function, alias).getValue();
            }
        }
        return super.recurseFunctions(query, object, defaultFieldType, fieldNameToFieldTypeMapping);
    }

}
