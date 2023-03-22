package com.github.xiao808.mongo.sql.visitor;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.ast.SQLAdhocTableSource;
import com.alibaba.druid.sql.ast.SQLAnnIndex;
import com.alibaba.druid.sql.ast.SQLArgument;
import com.alibaba.druid.sql.ast.SQLArrayDataType;
import com.alibaba.druid.sql.ast.SQLCommentHint;
import com.alibaba.druid.sql.ast.SQLCurrentTimeExpr;
import com.alibaba.druid.sql.ast.SQLCurrentUserExpr;
import com.alibaba.druid.sql.ast.SQLDataType;
import com.alibaba.druid.sql.ast.SQLDataTypeRefExpr;
import com.alibaba.druid.sql.ast.SQLDeclareItem;
import com.alibaba.druid.sql.ast.SQLIndexDefinition;
import com.alibaba.druid.sql.ast.SQLIndexOptions;
import com.alibaba.druid.sql.ast.SQLKeep;
import com.alibaba.druid.sql.ast.SQLLimit;
import com.alibaba.druid.sql.ast.SQLMapDataType;
import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.SQLOver;
import com.alibaba.druid.sql.ast.SQLParameter;
import com.alibaba.druid.sql.ast.SQLPartition;
import com.alibaba.druid.sql.ast.SQLPartitionByHash;
import com.alibaba.druid.sql.ast.SQLPartitionByList;
import com.alibaba.druid.sql.ast.SQLPartitionByRange;
import com.alibaba.druid.sql.ast.SQLPartitionByValue;
import com.alibaba.druid.sql.ast.SQLPartitionSpec;
import com.alibaba.druid.sql.ast.SQLPartitionValue;
import com.alibaba.druid.sql.ast.SQLRecordDataType;
import com.alibaba.druid.sql.ast.SQLRowDataType;
import com.alibaba.druid.sql.ast.SQLStructDataType;
import com.alibaba.druid.sql.ast.SQLSubPartition;
import com.alibaba.druid.sql.ast.SQLSubPartitionByHash;
import com.alibaba.druid.sql.ast.SQLSubPartitionByList;
import com.alibaba.druid.sql.ast.SQLSubPartitionByRange;
import com.alibaba.druid.sql.ast.SQLTableDataType;
import com.alibaba.druid.sql.ast.SQLUnionDataType;
import com.alibaba.druid.sql.ast.SQLWindow;
import com.alibaba.druid.sql.ast.SQLZOrderBy;
import com.alibaba.druid.sql.ast.expr.SQLAggregateExpr;
import com.alibaba.druid.sql.ast.expr.SQLAllColumnExpr;
import com.alibaba.druid.sql.ast.expr.SQLAllExpr;
import com.alibaba.druid.sql.ast.expr.SQLAnyExpr;
import com.alibaba.druid.sql.ast.expr.SQLArrayExpr;
import com.alibaba.druid.sql.ast.expr.SQLBetweenExpr;
import com.alibaba.druid.sql.ast.expr.SQLBigIntExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExprGroup;
import com.alibaba.druid.sql.ast.expr.SQLBooleanExpr;
import com.alibaba.druid.sql.ast.expr.SQLCaseExpr;
import com.alibaba.druid.sql.ast.expr.SQLCaseStatement;
import com.alibaba.druid.sql.ast.expr.SQLCastExpr;
import com.alibaba.druid.sql.ast.expr.SQLCharExpr;
import com.alibaba.druid.sql.ast.expr.SQLContainsExpr;
import com.alibaba.druid.sql.ast.expr.SQLCurrentOfCursorExpr;
import com.alibaba.druid.sql.ast.expr.SQLDateExpr;
import com.alibaba.druid.sql.ast.expr.SQLDateTimeExpr;
import com.alibaba.druid.sql.ast.expr.SQLDbLinkExpr;
import com.alibaba.druid.sql.ast.expr.SQLDecimalExpr;
import com.alibaba.druid.sql.ast.expr.SQLDefaultExpr;
import com.alibaba.druid.sql.ast.expr.SQLDoubleExpr;
import com.alibaba.druid.sql.ast.expr.SQLExistsExpr;
import com.alibaba.druid.sql.ast.expr.SQLExtractExpr;
import com.alibaba.druid.sql.ast.expr.SQLFlashbackExpr;
import com.alibaba.druid.sql.ast.expr.SQLFloatExpr;
import com.alibaba.druid.sql.ast.expr.SQLGroupingSetExpr;
import com.alibaba.druid.sql.ast.expr.SQLHexExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLInListExpr;
import com.alibaba.druid.sql.ast.expr.SQLInSubQueryExpr;
import com.alibaba.druid.sql.ast.expr.SQLIntegerExpr;
import com.alibaba.druid.sql.ast.expr.SQLIntervalExpr;
import com.alibaba.druid.sql.ast.expr.SQLJSONExpr;
import com.alibaba.druid.sql.ast.expr.SQLListExpr;
import com.alibaba.druid.sql.ast.expr.SQLMatchAgainstExpr;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;
import com.alibaba.druid.sql.ast.expr.SQLNCharExpr;
import com.alibaba.druid.sql.ast.expr.SQLNotExpr;
import com.alibaba.druid.sql.ast.expr.SQLNullExpr;
import com.alibaba.druid.sql.ast.expr.SQLNumberExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.expr.SQLQueryExpr;
import com.alibaba.druid.sql.ast.expr.SQLRealExpr;
import com.alibaba.druid.sql.ast.expr.SQLSequenceExpr;
import com.alibaba.druid.sql.ast.expr.SQLSizeExpr;
import com.alibaba.druid.sql.ast.expr.SQLSmallIntExpr;
import com.alibaba.druid.sql.ast.expr.SQLSomeExpr;
import com.alibaba.druid.sql.ast.expr.SQLTimeExpr;
import com.alibaba.druid.sql.ast.expr.SQLTimestampExpr;
import com.alibaba.druid.sql.ast.expr.SQLTinyIntExpr;
import com.alibaba.druid.sql.ast.expr.SQLUnaryExpr;
import com.alibaba.druid.sql.ast.expr.SQLValuesExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.statement.SQLAlterCharacter;
import com.alibaba.druid.sql.ast.statement.SQLAlterDatabaseStatement;
import com.alibaba.druid.sql.ast.statement.SQLAlterFunctionStatement;
import com.alibaba.druid.sql.ast.statement.SQLAlterIndexStatement;
import com.alibaba.druid.sql.ast.statement.SQLAlterMaterializedViewStatement;
import com.alibaba.druid.sql.ast.statement.SQLAlterOutlineStatement;
import com.alibaba.druid.sql.ast.statement.SQLAlterProcedureStatement;
import com.alibaba.druid.sql.ast.statement.SQLAlterSequenceStatement;
import com.alibaba.druid.sql.ast.statement.SQLAlterSystemGetConfigStatement;
import com.alibaba.druid.sql.ast.statement.SQLAlterSystemSetConfigStatement;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableAddClusteringKey;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableAddColumn;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableAddConstraint;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableAddExtPartition;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableAddIndex;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableAddPartition;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableAddSupplemental;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableAlterColumn;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableAlterIndex;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableAnalyzePartition;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableArchivePartition;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableBlockSize;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableChangeOwner;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableCheckPartition;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableCoalescePartition;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableCompression;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableConvertCharSet;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableDeleteByCondition;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableDisableConstraint;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableDisableKeys;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableDisableLifecycle;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableDiscardPartition;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableDropClusteringKey;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableDropColumnItem;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableDropConstraint;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableDropExtPartition;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableDropForeignKey;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableDropIndex;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableDropKey;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableDropPartition;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableDropPrimaryKey;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableDropSubpartition;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableEnableConstraint;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableEnableKeys;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableEnableLifecycle;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableExchangePartition;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableGroupStatement;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableImportPartition;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableMergePartition;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableModifyClusteredBy;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableOptimizePartition;
import com.alibaba.druid.sql.ast.statement.SQLAlterTablePartition;
import com.alibaba.druid.sql.ast.statement.SQLAlterTablePartitionCount;
import com.alibaba.druid.sql.ast.statement.SQLAlterTablePartitionLifecycle;
import com.alibaba.druid.sql.ast.statement.SQLAlterTablePartitionSetProperties;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableReOrganizePartition;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableRebuildPartition;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableRecoverPartitions;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableRename;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableRenameColumn;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableRenameIndex;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableRenamePartition;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableRepairPartition;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableReplaceColumn;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableSetComment;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableSetLifecycle;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableSetLocation;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableSetOption;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableStatement;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableSubpartitionAvailablePartitionNum;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableSubpartitionLifecycle;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableTouch;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableTruncatePartition;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableUnarchivePartition;
import com.alibaba.druid.sql.ast.statement.SQLAlterTypeStatement;
import com.alibaba.druid.sql.ast.statement.SQLAlterViewRenameStatement;
import com.alibaba.druid.sql.ast.statement.SQLAlterViewStatement;
import com.alibaba.druid.sql.ast.statement.SQLAnalyzeTableStatement;
import com.alibaba.druid.sql.ast.statement.SQLArchiveTableStatement;
import com.alibaba.druid.sql.ast.statement.SQLAssignItem;
import com.alibaba.druid.sql.ast.statement.SQLBackupStatement;
import com.alibaba.druid.sql.ast.statement.SQLBlockStatement;
import com.alibaba.druid.sql.ast.statement.SQLBuildTableStatement;
import com.alibaba.druid.sql.ast.statement.SQLCallStatement;
import com.alibaba.druid.sql.ast.statement.SQLCancelJobStatement;
import com.alibaba.druid.sql.ast.statement.SQLCharacterDataType;
import com.alibaba.druid.sql.ast.statement.SQLCheck;
import com.alibaba.druid.sql.ast.statement.SQLCloneTableStatement;
import com.alibaba.druid.sql.ast.statement.SQLCloseStatement;
import com.alibaba.druid.sql.ast.statement.SQLColumnCheck;
import com.alibaba.druid.sql.ast.statement.SQLColumnDefinition;
import com.alibaba.druid.sql.ast.statement.SQLColumnPrimaryKey;
import com.alibaba.druid.sql.ast.statement.SQLColumnReference;
import com.alibaba.druid.sql.ast.statement.SQLColumnUniqueKey;
import com.alibaba.druid.sql.ast.statement.SQLCommentStatement;
import com.alibaba.druid.sql.ast.statement.SQLCommitStatement;
import com.alibaba.druid.sql.ast.statement.SQLCopyFromStatement;
import com.alibaba.druid.sql.ast.statement.SQLCreateDatabaseStatement;
import com.alibaba.druid.sql.ast.statement.SQLCreateFunctionStatement;
import com.alibaba.druid.sql.ast.statement.SQLCreateIndexStatement;
import com.alibaba.druid.sql.ast.statement.SQLCreateMaterializedViewStatement;
import com.alibaba.druid.sql.ast.statement.SQLCreateOutlineStatement;
import com.alibaba.druid.sql.ast.statement.SQLCreateProcedureStatement;
import com.alibaba.druid.sql.ast.statement.SQLCreateRoleStatement;
import com.alibaba.druid.sql.ast.statement.SQLCreateSequenceStatement;
import com.alibaba.druid.sql.ast.statement.SQLCreateTableGroupStatement;
import com.alibaba.druid.sql.ast.statement.SQLCreateTableStatement;
import com.alibaba.druid.sql.ast.statement.SQLCreateTriggerStatement;
import com.alibaba.druid.sql.ast.statement.SQLCreateUserStatement;
import com.alibaba.druid.sql.ast.statement.SQLCreateViewStatement;
import com.alibaba.druid.sql.ast.statement.SQLDeclareStatement;
import com.alibaba.druid.sql.ast.statement.SQLDefault;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLDescribeStatement;
import com.alibaba.druid.sql.ast.statement.SQLDropCatalogStatement;
import com.alibaba.druid.sql.ast.statement.SQLDropDatabaseStatement;
import com.alibaba.druid.sql.ast.statement.SQLDropEventStatement;
import com.alibaba.druid.sql.ast.statement.SQLDropFunctionStatement;
import com.alibaba.druid.sql.ast.statement.SQLDropIndexStatement;
import com.alibaba.druid.sql.ast.statement.SQLDropLogFileGroupStatement;
import com.alibaba.druid.sql.ast.statement.SQLDropMaterializedViewStatement;
import com.alibaba.druid.sql.ast.statement.SQLDropOutlineStatement;
import com.alibaba.druid.sql.ast.statement.SQLDropProcedureStatement;
import com.alibaba.druid.sql.ast.statement.SQLDropResourceGroupStatement;
import com.alibaba.druid.sql.ast.statement.SQLDropResourceStatement;
import com.alibaba.druid.sql.ast.statement.SQLDropRoleStatement;
import com.alibaba.druid.sql.ast.statement.SQLDropSequenceStatement;
import com.alibaba.druid.sql.ast.statement.SQLDropServerStatement;
import com.alibaba.druid.sql.ast.statement.SQLDropSynonymStatement;
import com.alibaba.druid.sql.ast.statement.SQLDropTableGroupStatement;
import com.alibaba.druid.sql.ast.statement.SQLDropTableSpaceStatement;
import com.alibaba.druid.sql.ast.statement.SQLDropTableStatement;
import com.alibaba.druid.sql.ast.statement.SQLDropTriggerStatement;
import com.alibaba.druid.sql.ast.statement.SQLDropTypeStatement;
import com.alibaba.druid.sql.ast.statement.SQLDropUserStatement;
import com.alibaba.druid.sql.ast.statement.SQLDropViewStatement;
import com.alibaba.druid.sql.ast.statement.SQLDumpStatement;
import com.alibaba.druid.sql.ast.statement.SQLErrorLoggingClause;
import com.alibaba.druid.sql.ast.statement.SQLExplainAnalyzeStatement;
import com.alibaba.druid.sql.ast.statement.SQLExplainStatement;
import com.alibaba.druid.sql.ast.statement.SQLExportDatabaseStatement;
import com.alibaba.druid.sql.ast.statement.SQLExportTableStatement;
import com.alibaba.druid.sql.ast.statement.SQLExprHint;
import com.alibaba.druid.sql.ast.statement.SQLExprStatement;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLExternalRecordFormat;
import com.alibaba.druid.sql.ast.statement.SQLFetchStatement;
import com.alibaba.druid.sql.ast.statement.SQLForStatement;
import com.alibaba.druid.sql.ast.statement.SQLForeignKeyImpl;
import com.alibaba.druid.sql.ast.statement.SQLGrantStatement;
import com.alibaba.druid.sql.ast.statement.SQLIfStatement;
import com.alibaba.druid.sql.ast.statement.SQLImportDatabaseStatement;
import com.alibaba.druid.sql.ast.statement.SQLImportTableStatement;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLLateralViewTableSource;
import com.alibaba.druid.sql.ast.statement.SQLLoopStatement;
import com.alibaba.druid.sql.ast.statement.SQLMergeStatement;
import com.alibaba.druid.sql.ast.statement.SQLNotNullConstraint;
import com.alibaba.druid.sql.ast.statement.SQLNullConstraint;
import com.alibaba.druid.sql.ast.statement.SQLOpenStatement;
import com.alibaba.druid.sql.ast.statement.SQLPartitionRef;
import com.alibaba.druid.sql.ast.statement.SQLPrimaryKeyImpl;
import com.alibaba.druid.sql.ast.statement.SQLPrivilegeItem;
import com.alibaba.druid.sql.ast.statement.SQLPurgeLogsStatement;
import com.alibaba.druid.sql.ast.statement.SQLPurgeRecyclebinStatement;
import com.alibaba.druid.sql.ast.statement.SQLPurgeTableStatement;
import com.alibaba.druid.sql.ast.statement.SQLPurgeTemporaryOutputStatement;
import com.alibaba.druid.sql.ast.statement.SQLRefreshMaterializedViewStatement;
import com.alibaba.druid.sql.ast.statement.SQLReleaseSavePointStatement;
import com.alibaba.druid.sql.ast.statement.SQLRenameUserStatement;
import com.alibaba.druid.sql.ast.statement.SQLReplaceStatement;
import com.alibaba.druid.sql.ast.statement.SQLRestoreStatement;
import com.alibaba.druid.sql.ast.statement.SQLReturnStatement;
import com.alibaba.druid.sql.ast.statement.SQLRevokeStatement;
import com.alibaba.druid.sql.ast.statement.SQLRollbackStatement;
import com.alibaba.druid.sql.ast.statement.SQLSavePointStatement;
import com.alibaba.druid.sql.ast.statement.SQLScriptCommitStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectGroupByClause;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectOrderByItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLSetStatement;
import com.alibaba.druid.sql.ast.statement.SQLShowACLStatement;
import com.alibaba.druid.sql.ast.statement.SQLShowCatalogsStatement;
import com.alibaba.druid.sql.ast.statement.SQLShowColumnsStatement;
import com.alibaba.druid.sql.ast.statement.SQLShowCreateMaterializedViewStatement;
import com.alibaba.druid.sql.ast.statement.SQLShowCreateTableStatement;
import com.alibaba.druid.sql.ast.statement.SQLShowCreateViewStatement;
import com.alibaba.druid.sql.ast.statement.SQLShowDatabasesStatement;
import com.alibaba.druid.sql.ast.statement.SQLShowErrorsStatement;
import com.alibaba.druid.sql.ast.statement.SQLShowFunctionsStatement;
import com.alibaba.druid.sql.ast.statement.SQLShowGrantsStatement;
import com.alibaba.druid.sql.ast.statement.SQLShowHistoryStatement;
import com.alibaba.druid.sql.ast.statement.SQLShowIndexesStatement;
import com.alibaba.druid.sql.ast.statement.SQLShowMaterializedViewStatement;
import com.alibaba.druid.sql.ast.statement.SQLShowOutlinesStatement;
import com.alibaba.druid.sql.ast.statement.SQLShowPackagesStatement;
import com.alibaba.druid.sql.ast.statement.SQLShowPartitionsStmt;
import com.alibaba.druid.sql.ast.statement.SQLShowProcessListStatement;
import com.alibaba.druid.sql.ast.statement.SQLShowQueryTaskStatement;
import com.alibaba.druid.sql.ast.statement.SQLShowRecylebinStatement;
import com.alibaba.druid.sql.ast.statement.SQLShowRoleStatement;
import com.alibaba.druid.sql.ast.statement.SQLShowRolesStatement;
import com.alibaba.druid.sql.ast.statement.SQLShowSessionStatement;
import com.alibaba.druid.sql.ast.statement.SQLShowStatisticListStmt;
import com.alibaba.druid.sql.ast.statement.SQLShowStatisticStmt;
import com.alibaba.druid.sql.ast.statement.SQLShowTableGroupsStatement;
import com.alibaba.druid.sql.ast.statement.SQLShowTablesStatement;
import com.alibaba.druid.sql.ast.statement.SQLShowUsersStatement;
import com.alibaba.druid.sql.ast.statement.SQLShowVariantsStatement;
import com.alibaba.druid.sql.ast.statement.SQLShowViewsStatement;
import com.alibaba.druid.sql.ast.statement.SQLStartTransactionStatement;
import com.alibaba.druid.sql.ast.statement.SQLSubmitJobStatement;
import com.alibaba.druid.sql.ast.statement.SQLSubqueryTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSyncMetaStatement;
import com.alibaba.druid.sql.ast.statement.SQLTableLike;
import com.alibaba.druid.sql.ast.statement.SQLTableSampling;
import com.alibaba.druid.sql.ast.statement.SQLTruncateStatement;
import com.alibaba.druid.sql.ast.statement.SQLUnionQuery;
import com.alibaba.druid.sql.ast.statement.SQLUnionQueryTableSource;
import com.alibaba.druid.sql.ast.statement.SQLUnique;
import com.alibaba.druid.sql.ast.statement.SQLUnnestTableSource;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.ast.statement.SQLUseStatement;
import com.alibaba.druid.sql.ast.statement.SQLValuesQuery;
import com.alibaba.druid.sql.ast.statement.SQLValuesTableSource;
import com.alibaba.druid.sql.ast.statement.SQLWhileStatement;
import com.alibaba.druid.sql.ast.statement.SQLWhoamiStatement;
import com.alibaba.druid.sql.ast.statement.SQLWithSubqueryClause;
import com.alibaba.druid.sql.dialect.hive.ast.HiveInputOutputFormat;
import com.alibaba.druid.sql.dialect.hive.stmt.HiveCreateTableStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlKillStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.SQLAlterResourceGroupStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.SQLCreateResourceGroupStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.SQLListResourceGroupStatement;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;

/**
 * @author zengxiao
 * @description sql transform to mongodb bson
 * @date 2023/3/22 14:36
 * @since 1.0
 **/
public class SqlTransformFromMysqlToMongoVisitor implements SqlTransformToMongoVisitor {

    /**
     * bson writer mode config
     */
    private static final JsonWriterSettings RELAXED = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();

    @Override
    public String getSrcDbType() {
        return DbType.mysql.name();
    }

    @Override
    public void endVisit(SQLAllColumnExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLBetweenExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLBinaryOpExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLCaseExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLCaseExpr.Item x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLCaseStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLCaseStatement.Item x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLCharExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLIdentifierExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLInListExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLIntegerExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLSmallIntExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLBigIntExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLTinyIntExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLExistsExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLNCharExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLNotExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLNullExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLNumberExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLRealExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLPropertyExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLSelectGroupByClause x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLSelectItem x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLSelectStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void postVisit(SQLObject x) {
        SqlTransformToMongoVisitor.super.postVisit(x);
    }

    @Override
    public void preVisit(SQLObject x) {
        SqlTransformToMongoVisitor.super.preVisit(x);
    }

    @Override
    public boolean visit(SQLAllColumnExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLBetweenExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLBinaryOpExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLCaseExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLCaseExpr.Item x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLCaseStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLCaseStatement.Item x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLCastExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLCharExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLExistsExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLIdentifierExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLInListExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLIntegerExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLSmallIntExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLBigIntExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLTinyIntExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLNCharExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLNotExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLNullExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLNumberExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLRealExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLPropertyExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLSelectGroupByClause x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLSelectItem x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLCastExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLSelectStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAggregateExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAggregateExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLVariantRefExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLVariantRefExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLQueryExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLQueryExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLUnaryExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLUnaryExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLHexExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLHexExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLSelect x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLSelect select) {
        SqlTransformToMongoVisitor.super.endVisit(select);
    }

    @Override
    public boolean visit(SQLSelectQueryBlock x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLSelectQueryBlock x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLExprTableSource x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLExprTableSource x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLOrderBy x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLOrderBy x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLZOrderBy x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLZOrderBy x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLSelectOrderByItem x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLSelectOrderByItem x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDropTableStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDropTableStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLCreateTableStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLCreateTableStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLColumnDefinition x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLColumnDefinition x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLColumnDefinition.Identity x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLColumnDefinition.Identity x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDataType x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDataType x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLCharacterDataType x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLCharacterDataType x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDeleteStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDeleteStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLCurrentOfCursorExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLCurrentOfCursorExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLInsertStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLInsertStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLInsertStatement.ValuesClause x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLInsertStatement.ValuesClause x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLUpdateSetItem x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLUpdateSetItem x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLUpdateStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLUpdateStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLCreateViewStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLCreateViewStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLCreateViewStatement.Column x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLCreateViewStatement.Column x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLNotNullConstraint x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLNotNullConstraint x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLMethodInvokeExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLMethodInvokeExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLUnionQuery x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLUnionQuery x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLSetStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLSetStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAssignItem x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAssignItem x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLCallStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLCallStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLJoinTableSource x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLJoinTableSource x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLJoinTableSource.UDJ x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLJoinTableSource.UDJ x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLSomeExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLSomeExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAnyExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAnyExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAllExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAllExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLInSubQueryExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLInSubQueryExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLListExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLListExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLSubqueryTableSource x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLSubqueryTableSource x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLTruncateStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLTruncateStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDefaultExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDefaultExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLCommentStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLCommentStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLUseStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLUseStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLAlterTableAddColumn x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableAddColumn x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableDeleteByCondition x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableDeleteByCondition x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableModifyClusteredBy x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableModifyClusteredBy x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableDropColumnItem x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableDropColumnItem x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableDropIndex x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableDropIndex x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableGroupStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableGroupStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterSystemSetConfigStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterSystemSetConfigStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterSystemGetConfigStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterSystemGetConfigStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDropIndexStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDropIndexStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDropViewStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDropViewStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLSavePointStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLSavePointStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLRollbackStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLRollbackStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLReleaseSavePointStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLReleaseSavePointStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLCommentHint x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLCommentHint x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLCreateDatabaseStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLCreateDatabaseStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLOver x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLOver x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLKeep x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLKeep x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLColumnPrimaryKey x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLColumnPrimaryKey x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLColumnUniqueKey x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLColumnUniqueKey x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLWithSubqueryClause x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLWithSubqueryClause x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLWithSubqueryClause.Entry x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLWithSubqueryClause.Entry x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableAlterColumn x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableAlterColumn x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLCheck x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLCheck x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDefault x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDefault x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableDropForeignKey x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableDropForeignKey x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableDropPrimaryKey x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableDropPrimaryKey x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableDisableKeys x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableDisableKeys x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableEnableKeys x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableEnableKeys x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableDisableConstraint x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableDisableConstraint x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableEnableConstraint x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableEnableConstraint x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLColumnCheck x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLColumnCheck x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLExprHint x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLExprHint x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableDropConstraint x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableDropConstraint x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLUnique x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLUnique x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLPrimaryKeyImpl x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLPrimaryKeyImpl x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLCreateIndexStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLCreateIndexStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableRenameColumn x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableRenameColumn x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLColumnReference x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLColumnReference x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLForeignKeyImpl x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLForeignKeyImpl x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDropSequenceStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDropSequenceStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDropTriggerStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDropTriggerStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLDropUserStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDropUserStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLExplainStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLExplainStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLGrantStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLGrantStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDropDatabaseStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDropDatabaseStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLIndexOptions x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLIndexOptions x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLIndexDefinition x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLIndexDefinition x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableAddIndex x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableAddIndex x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableAlterIndex x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableAlterIndex x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableAddConstraint x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableAddConstraint x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLCreateTriggerStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLCreateTriggerStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDropFunctionStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDropFunctionStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDropTableSpaceStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDropTableSpaceStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDropProcedureStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDropProcedureStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLBooleanExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLBooleanExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLUnionQueryTableSource x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLUnionQueryTableSource x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLTimestampExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLTimestampExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDateTimeExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDateTimeExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDoubleExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDoubleExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLFloatExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLFloatExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLRevokeStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLRevokeStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLBinaryExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLBinaryExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableRename x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableRename x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterViewRenameStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterViewRenameStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLShowTablesStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLShowTablesStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableAddPartition x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableAddPartition x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableAddExtPartition x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableAddExtPartition x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableDropExtPartition x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableDropExtPartition x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableDropPartition x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableDropPartition x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableRenamePartition x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableRenamePartition x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableSetComment x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableSetComment x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableSetLifecycle x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLPrivilegeItem x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLPrivilegeItem x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableSetLifecycle x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableEnableLifecycle x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableSetLocation x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableSetLocation x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableEnableLifecycle x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTablePartition x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTablePartition x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTablePartitionSetProperties x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTablePartitionSetProperties x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableDisableLifecycle x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableDisableLifecycle x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableTouch x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableTouch x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLArrayExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLArrayExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLOpenStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLOpenStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLFetchStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLFetchStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLCloseStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLCloseStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLGroupingSetExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLGroupingSetExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLIfStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLIfStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLIfStatement.ElseIf x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLIfStatement.ElseIf x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLIfStatement.Else x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLIfStatement.Else x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLLoopStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLLoopStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLParameter x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLParameter x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLCreateProcedureStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLCreateProcedureStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLCreateFunctionStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLCreateFunctionStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLBlockStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLBlockStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableDropKey x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableDropKey x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDeclareItem x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDeclareItem x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLPartitionValue x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLPartitionValue x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLPartition x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLPartition x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLPartitionByRange x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLPartitionByRange x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLPartitionByHash x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLPartitionByHash x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLPartitionByList x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLPartitionByList x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLSubPartition x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLSubPartition x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLSubPartitionByHash x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLSubPartitionByHash x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLSubPartitionByRange x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLSubPartitionByRange x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLSubPartitionByList x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLSubPartitionByList x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterDatabaseStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterDatabaseStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableConvertCharSet x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableConvertCharSet x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableReOrganizePartition x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableReOrganizePartition x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableCoalescePartition x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableCoalescePartition x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableTruncatePartition x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableTruncatePartition x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableDiscardPartition x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableDiscardPartition x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableImportPartition x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableImportPartition x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableAnalyzePartition x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableAnalyzePartition x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableCheckPartition x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableCheckPartition x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableOptimizePartition x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableOptimizePartition x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableRebuildPartition x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableRebuildPartition x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableRepairPartition x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableRepairPartition x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLSequenceExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLSequenceExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLMergeStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLMergeStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLMergeStatement.MergeUpdateClause x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLMergeStatement.MergeUpdateClause x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLMergeStatement.MergeInsertClause x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLMergeStatement.MergeInsertClause x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLErrorLoggingClause x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLErrorLoggingClause x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLNullConstraint x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLNullConstraint x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLCreateSequenceStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLCreateSequenceStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDateExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDateExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLLimit x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLLimit x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLStartTransactionStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLStartTransactionStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDescribeStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDescribeStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLWhileStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLWhileStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDeclareStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDeclareStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLReturnStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLReturnStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLArgument x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLArgument x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLCommitStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLCommitStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLFlashbackExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLFlashbackExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLCreateMaterializedViewStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLCreateMaterializedViewStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLShowCreateMaterializedViewStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLShowCreateMaterializedViewStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLBinaryOpExprGroup x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLBinaryOpExprGroup x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLScriptCommitStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLScriptCommitStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLReplaceStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLReplaceStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLCreateUserStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLCreateUserStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterFunctionStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterFunctionStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTypeStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTypeStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLIntervalExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLIntervalExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLLateralViewTableSource x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLLateralViewTableSource x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLShowErrorsStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLShowErrorsStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLShowGrantsStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLShowGrantsStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLShowPackagesStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLShowPackagesStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLShowRecylebinStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLShowRecylebinStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterCharacter x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterCharacter x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLExprStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLExprStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterProcedureStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterProcedureStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterViewStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterViewStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDropEventStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDropEventStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDropLogFileGroupStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDropLogFileGroupStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDropServerStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDropServerStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDropSynonymStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDropSynonymStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLRecordDataType x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLRecordDataType x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDropTypeStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDropTypeStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLExternalRecordFormat x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLExternalRecordFormat x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLArrayDataType x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLArrayDataType x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLMapDataType x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLMapDataType x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLStructDataType x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLStructDataType x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLRowDataType x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLRowDataType x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLStructDataType.Field x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLStructDataType.Field x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDropMaterializedViewStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDropMaterializedViewStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLShowMaterializedViewStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLShowMaterializedViewStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLRefreshMaterializedViewStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLRefreshMaterializedViewStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterMaterializedViewStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterMaterializedViewStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLCreateTableGroupStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLCreateTableGroupStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDropTableGroupStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDropTableGroupStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableSubpartitionAvailablePartitionNum x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableSubpartitionAvailablePartitionNum x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLShowDatabasesStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLShowDatabasesStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLShowTableGroupsStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLShowTableGroupsStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLShowColumnsStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLShowColumnsStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLShowCreateTableStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLShowCreateTableStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLShowProcessListStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLShowProcessListStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableSetOption x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableSetOption x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLShowCreateViewStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLShowCreateViewStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLShowViewsStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLShowViewsStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableRenameIndex x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableRenameIndex x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterSequenceStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterSequenceStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableExchangePartition x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableExchangePartition x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLCreateRoleStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLCreateRoleStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDropRoleStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDropRoleStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableReplaceColumn x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableReplaceColumn x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLMatchAgainstExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLMatchAgainstExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLTimeExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLTimeExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDropCatalogStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDropCatalogStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLShowPartitionsStmt x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLShowPartitionsStmt x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLValuesExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLValuesExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLContainsExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLContainsExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDumpStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDumpStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLValuesTableSource x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLValuesTableSource x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLExtractExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLExtractExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLWindow x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLWindow x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLJSONExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLJSONExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDecimalExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDecimalExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAnnIndex x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAnnIndex x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLUnionDataType x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLUnionDataType x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableRecoverPartitions x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableRecoverPartitions x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterIndexStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterIndexStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLAlterIndexStatement.Rebuild x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterIndexStatement.Rebuild x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLShowIndexesStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLShowIndexesStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAnalyzeTableStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAnalyzeTableStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLExportTableStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLExportTableStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLImportTableStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLImportTableStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLTableSampling x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLTableSampling x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLSizeExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLSizeExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableArchivePartition x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableArchivePartition x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableUnarchivePartition x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableUnarchivePartition x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLCreateOutlineStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLCreateOutlineStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDropOutlineStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDropOutlineStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterOutlineStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterOutlineStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLShowOutlinesStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLShowOutlinesStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLPurgeTableStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLPurgeTableStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLPurgeTemporaryOutputStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLPurgeTemporaryOutputStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLPurgeLogsStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLPurgeLogsStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLPurgeRecyclebinStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLPurgeRecyclebinStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLShowStatisticStmt x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLShowStatisticStmt x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLShowStatisticListStmt x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLShowStatisticListStmt x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableAddSupplemental x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableAddSupplemental x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLShowCatalogsStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLShowCatalogsStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLShowFunctionsStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLShowFunctionsStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLShowSessionStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLShowSessionStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDbLinkExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDbLinkExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLCurrentTimeExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLCurrentTimeExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLCurrentUserExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLCurrentUserExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLShowQueryTaskStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLShowQueryTaskStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAdhocTableSource x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAdhocTableSource x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(HiveCreateTableStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(HiveCreateTableStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(HiveInputOutputFormat x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(HiveInputOutputFormat x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLExplainAnalyzeStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLExplainAnalyzeStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLPartitionRef x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLPartitionRef x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLPartitionRef.Item x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLPartitionRef.Item x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLWhoamiStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLWhoamiStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDropResourceStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDropResourceStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLForStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLForStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLUnnestTableSource x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLUnnestTableSource x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLCopyFromStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLCopyFromStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLShowUsersStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLShowUsersStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLSubmitJobStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLSubmitJobStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLTableLike x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLTableLike x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLSyncMetaStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLSyncMetaStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLValuesQuery x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLValuesQuery x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLDataTypeRefExpr x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDataTypeRefExpr x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLArchiveTableStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLArchiveTableStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLBackupStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLBackupStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLRestoreStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLRestoreStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLBuildTableStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLBuildTableStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLCancelJobStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLCancelJobStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLExportDatabaseStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLExportDatabaseStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLImportDatabaseStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLImportDatabaseStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLRenameUserStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLRenameUserStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLPartitionByValue x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLPartitionByValue x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTablePartitionCount x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTablePartitionCount x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableBlockSize x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableBlockSize x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableCompression x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableCompression x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTablePartitionLifecycle x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTablePartitionLifecycle x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableSubpartitionLifecycle x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableSubpartitionLifecycle x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableDropSubpartition x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableDropSubpartition x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableDropClusteringKey x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableDropClusteringKey x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableAddClusteringKey x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableAddClusteringKey x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(MySqlKillStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(MySqlKillStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLCreateResourceGroupStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLCreateResourceGroupStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterResourceGroupStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterResourceGroupStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public void endVisit(SQLDropResourceGroupStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLDropResourceGroupStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLListResourceGroupStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLListResourceGroupStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableMergePartition x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableMergePartition x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLPartitionSpec x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLPartitionSpec x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLPartitionSpec.Item x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLPartitionSpec.Item x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLAlterTableChangeOwner x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLAlterTableChangeOwner x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLTableDataType x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLTableDataType x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLCloneTableStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLCloneTableStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLShowHistoryStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLShowHistoryStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLShowRoleStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLShowRoleStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLShowRolesStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLShowRolesStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public boolean visit(SQLShowVariantsStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLShowVariantsStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }

    @Override
    public boolean visit(SQLShowACLStatement x) {
        return SqlTransformToMongoVisitor.super.visit(x);
    }

    @Override
    public void endVisit(SQLShowACLStatement x) {
        SqlTransformToMongoVisitor.super.endVisit(x);
    }
}
