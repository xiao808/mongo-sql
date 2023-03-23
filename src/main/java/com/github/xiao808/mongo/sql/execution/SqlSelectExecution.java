package com.github.xiao808.mongo.sql.execution;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectOrderByItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectQuery;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLWithSubqueryClause;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.xiao808.mongo.sql.InheritableThreadLocalMongoContextHolder;
import com.github.xiao808.mongo.sql.MongoContext;
import com.mongodb.client.MongoDatabase;

/**
 * @author zengxiao
 * @description
 * @date 2023/3/22 15:44
 * @since 1.0
 **/
public class SqlSelectExecution implements SqlExecution {

    @Override
    public JsonNode execute(MongoDatabase mongoDatabase) {
        MongoContext mongoContext = InheritableThreadLocalMongoContextHolder.getContext();
        SQLSelectStatement sqlStatement = (SQLSelectStatement) mongoContext.getSqlStatement();
        SQLSelect select = sqlStatement.getSelect();
        SQLWithSubqueryClause with = select.getWithSubQuery();
        if (with != null) {
        }

        SQLSelectQuery query = select.getQuery();
        if (query != null) {
            if (query instanceof SQLSelectQueryBlock) {
            } else {
            }
        }

        SQLSelectQueryBlock queryBlock = select.getFirstQueryBlock();

        SQLOrderBy orderBy = select.getOrderBy();
        if (orderBy != null) {
            for (SQLSelectOrderByItem orderByItem : orderBy.getItems()) {
                SQLExpr orderByItemExpr = orderByItem.getExpr();

                if (orderByItemExpr instanceof SQLIdentifierExpr) {
                    SQLIdentifierExpr orderByItemIdentExpr = (SQLIdentifierExpr) orderByItemExpr;
                    long hash = orderByItemIdentExpr.nameHashCode64();

                    SQLSelectItem selectItem = null;
                    if (queryBlock != null) {
                        selectItem = queryBlock.findSelectItem(hash);
                    }

                    if (selectItem != null) {
                        orderByItem.setResolvedSelectItem(selectItem);

                        SQLExpr selectItemExpr = selectItem.getExpr();
                        if (selectItemExpr instanceof SQLIdentifierExpr) {
                            orderByItemIdentExpr.setResolvedTableSource(((SQLIdentifierExpr) selectItemExpr).getResolvedTableSource());
                            orderByItemIdentExpr.setResolvedColumn(((SQLIdentifierExpr) selectItemExpr).getResolvedColumn());
                        } else if (selectItemExpr instanceof SQLPropertyExpr) {
                            orderByItemIdentExpr.setResolvedTableSource(((SQLPropertyExpr) selectItemExpr).getResolvedTableSource());
                            orderByItemIdentExpr.setResolvedColumn(((SQLPropertyExpr) selectItemExpr).getResolvedColumn());
                        }
                        continue;
                    }
                }

            }
        }
        return null;
    }
}
