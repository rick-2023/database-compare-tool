package com.dbdiff.plugin.model;

import java.util.List;

public class TableDiff {
    private String tableName;
    private DiffType diffType;
    private List<ColumnDiff> columnDiffs;
    private String createTableSql;
    
    public enum DiffType {
        ADDED,
        REMOVED,
        MODIFIED
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
    
    public DiffType getDiffType() {
        return diffType;
    }
    
    public void setDiffType(DiffType diffType) {
        this.diffType = diffType;
    }
    
    public List<ColumnDiff> getColumnDiffs() {
        return columnDiffs;
    }
    
    public void setColumnDiffs(List<ColumnDiff> columnDiffs) {
        this.columnDiffs = columnDiffs;
    }
    
    public String getCreateTableSql() {
        return createTableSql;
    }
    
    public void setCreateTableSql(String createTableSql) {
        this.createTableSql = createTableSql;
    }
} 