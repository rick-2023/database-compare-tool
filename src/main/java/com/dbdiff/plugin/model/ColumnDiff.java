package com.dbdiff.plugin.model;

public class ColumnDiff {
    private String columnName;
    private String sourceType;
    private String targetType;
    private DiffType diffType;
    
    public enum DiffType {
        ADDED,
        REMOVED,
        TYPE_CHANGED
    }
    
    public String getColumnName() {
        return columnName;
    }
    
    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }
    
    public String getSourceType() {
        return sourceType;
    }
    
    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }
    
    public String getTargetType() {
        return targetType;
    }
    
    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }
    
    public DiffType getDiffType() {
        return diffType;
    }
    
    public void setDiffType(DiffType diffType) {
        this.diffType = diffType;
    }
} 