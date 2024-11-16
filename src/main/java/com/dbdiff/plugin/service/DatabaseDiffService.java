package com.dbdiff.plugin.service;

import com.dbdiff.plugin.model.TableDiff;
import com.dbdiff.plugin.model.ColumnDiff;
import com.dbdiff.plugin.exception.DatabaseComparisonException;
import com.intellij.openapi.diagnostic.Logger;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class DatabaseDiffService {
    private static final Logger LOG = Logger.getInstance(DatabaseDiffService.class);
    private static final String[] TABLE_TYPES = new String[]{"TABLE"};
    private static final String SQL_SHOW_CREATE_TABLE = "SHOW CREATE TABLE ";
    private static final int CREATE_TABLE_COLUMN_INDEX = 2;

    public List<TableDiff> compareDatabase(Connection source, Connection target) {
        try {
            validateConnections(source, target);
            return doCompareDatabase(source, target);
        } catch (SQLException e) {
            LOG.error("Database comparison failed", e);
            throw new DatabaseComparisonException("Failed to compare databases", e);
        }
    }

    private void validateConnections(Connection source, Connection target) throws SQLException {
        if (source == null || target == null) {
            throw new IllegalArgumentException("Database connections cannot be null");
        }
        if (source.isClosed() || target.isClosed()) {
            throw new IllegalStateException("Database connections must be open");
        }
    }

    private List<TableDiff> doCompareDatabase(Connection source, Connection target) throws SQLException {
        DatabaseMetaData sourceMetaData = source.getMetaData();
        DatabaseMetaData targetMetaData = target.getMetaData();
        
        String sourceCatalog = source.getCatalog();
        String targetCatalog = target.getCatalog();
        
        Map<String, Set<String>> sourceTables = getTables(sourceMetaData);
        Map<String, Set<String>> targetTables = getTables(targetMetaData);
        
        Set<String> srcTables = sourceTables.getOrDefault(sourceCatalog, new HashSet<>());
        Set<String> tgtTables = targetTables.getOrDefault(targetCatalog, new HashSet<>());

        List<TableDiff> diffs = new ArrayList<>();
        processRemovedTables(source, sourceCatalog, srcTables, tgtTables, diffs);
        processAddedTables(target, targetCatalog, srcTables, tgtTables, diffs);
        processModifiedTables(sourceMetaData, targetMetaData, sourceCatalog, targetCatalog, 
                            srcTables, tgtTables, diffs);

        return diffs;
    }

    private Map<String, Set<String>> getTables(DatabaseMetaData metaData) throws SQLException {
        Map<String, Set<String>> tables = new HashMap<>();
        try (ResultSet rs = metaData.getTables(null, null, "%", TABLE_TYPES)) {
            while (rs.next()) {
                String catalog = rs.getString("TABLE_CAT");
                String tableName = rs.getString("TABLE_NAME");
                tables.computeIfAbsent(catalog, k -> new HashSet<>()).add(tableName);
            }
        }
        return tables;
    }

    private List<ColumnDiff> compareColumns(
            DatabaseMetaData sourceMetaData,
            DatabaseMetaData targetMetaData,
            String sourceCatalog,
            String targetCatalog,
            String table) throws SQLException {
        
        List<ColumnDiff> diffs = new ArrayList<>();
        Map<String, ColumnInfo> sourceColumns = getColumns(sourceMetaData, sourceCatalog, table);
        Map<String, ColumnInfo> targetColumns = getColumns(targetMetaData, targetCatalog, table);

        LOG.info("Comparing columns for table: " + table);
        LOG.info("Source columns: " + sourceColumns.keySet());
        LOG.info("Target columns: " + targetColumns.keySet());

        // 检查���的变化
        for (Map.Entry<String, ColumnInfo> entry : sourceColumns.entrySet()) {
            String columnName = entry.getKey();
            ColumnInfo sourceColumn = entry.getValue();
            ColumnInfo targetColumn = targetColumns.get(columnName);

            if (targetColumn == null) {
                // 列被删除
                ColumnDiff diff = new ColumnDiff();
                diff.setColumnName(columnName);
                diff.setSourceType(sourceColumn.toString());
                diff.setDiffType(ColumnDiff.DiffType.REMOVED);
                diffs.add(diff);
                LOG.info("Found removed column: " + columnName + " in table: " + table);
            } else if (!sourceColumn.equals(targetColumn)) {
                // 列类型改变
                ColumnDiff diff = new ColumnDiff();
                diff.setColumnName(columnName);
                diff.setSourceType(sourceColumn.toString());
                diff.setTargetType(targetColumn.toString());
                diff.setDiffType(ColumnDiff.DiffType.TYPE_CHANGED);
                diffs.add(diff);
                LOG.info("Found modified column: " + columnName + " in table: " + table + 
                        " from " + sourceColumn + " to " + targetColumn);
            }
        }

        // 检查新增的列
        for (String columnName : targetColumns.keySet()) {
            if (!sourceColumns.containsKey(columnName)) {
                ColumnDiff diff = new ColumnDiff();
                diff.setColumnName(columnName);
                diff.setTargetType(targetColumns.get(columnName).toString());
                diff.setDiffType(ColumnDiff.DiffType.ADDED);
                diffs.add(diff);
                LOG.info("Found added column: " + columnName + " in table: " + table);
            }
        }

        return diffs;
    }

    private static class ColumnInfo {
        String type;
        int size;
        int decimal;
        boolean nullable;
        String defaultValue;
        String comment;

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ColumnInfo)) return false;
            ColumnInfo other = (ColumnInfo) obj;
            return Objects.equals(type, other.type) &&
                   size == other.size &&
                   decimal == other.decimal &&
                   nullable == other.nullable &&
                   Objects.equals(defaultValue, other.defaultValue);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(type);
            if (size > 0) {
                sb.append("(").append(size);
                if (decimal > 0) {
                    sb.append(",").append(decimal);
                }
                sb.append(")");
            }
            if (!nullable) {
                sb.append(" NOT NULL");
            }
            if (defaultValue != null) {
                sb.append(" DEFAULT ").append(defaultValue);
            }
            return sb.toString();
        }
    }

    private Map<String, ColumnInfo> getColumns(
            DatabaseMetaData metaData,
            String catalog,
            String table) throws SQLException {
        
        Map<String, ColumnInfo> columns = new HashMap<>();
        try (ResultSet rs = metaData.getColumns(catalog, null, table, null)) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                ColumnInfo info = new ColumnInfo();
                info.type = rs.getString("TYPE_NAME");
                info.size = rs.getInt("COLUMN_SIZE");
                info.decimal = rs.getInt("DECIMAL_DIGITS");
                info.nullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                info.defaultValue = rs.getString("COLUMN_DEF");
                info.comment = rs.getString("REMARKS");
                columns.put(columnName, info);
            }
        }
        return columns;
    }

    private String getCreateTableSql(Connection conn, String catalog, String table) throws SQLException {
        String sql = SQL_SHOW_CREATE_TABLE + table;
        try (java.sql.Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getString(CREATE_TABLE_COLUMN_INDEX); // 第二列是CREATE TABLE语句
            }
        }
        return null;
    }

    private void processRemovedTables(Connection source, String sourceCatalog,
                                    Set<String> srcTables, Set<String> tgtTables,
                                    List<TableDiff> diffs) throws SQLException {
        for (String table : srcTables) {
            if (!tgtTables.contains(table)) {
                TableDiff diff = new TableDiff();
                diff.setTableName(table);
                diff.setDiffType(TableDiff.DiffType.REMOVED);
                diff.setCreateTableSql(getCreateTableSql(source, sourceCatalog, table));
                diffs.add(diff);
                LOG.info("Found removed table: " + table);
            }
        }
    }

    private void processAddedTables(Connection target, String targetCatalog,
                                  Set<String> srcTables, Set<String> tgtTables,
                                  List<TableDiff> diffs) throws SQLException {
        for (String table : tgtTables) {
            if (!srcTables.contains(table)) {
                TableDiff diff = new TableDiff();
                diff.setTableName(table);
                diff.setDiffType(TableDiff.DiffType.ADDED);
                diff.setCreateTableSql(getCreateTableSql(target, targetCatalog, table));
                diffs.add(diff);
                LOG.info("Found added table: " + table);
            }
        }
    }

    private void processModifiedTables(DatabaseMetaData sourceMetaData,
                                     DatabaseMetaData targetMetaData,
                                     String sourceCatalog,
                                     String targetCatalog,
                                     Set<String> srcTables,
                                     Set<String> tgtTables,
                                     List<TableDiff> diffs) throws SQLException {
        // 处理共同存在的表
        Set<String> commonTables = new HashSet<>(srcTables);
        commonTables.retainAll(tgtTables);

        for (String table : commonTables) {
            List<ColumnDiff> columnDiffs = compareColumns(
                sourceMetaData, targetMetaData, sourceCatalog, targetCatalog, table);
            
            if (!columnDiffs.isEmpty()) {
                TableDiff diff = new TableDiff();
                diff.setTableName(table);
                diff.setDiffType(TableDiff.DiffType.MODIFIED);
                diff.setColumnDiffs(columnDiffs);
                diffs.add(diff);
                LOG.info("Found modified table: " + table + " with " + columnDiffs.size() + " changes");
            }
        }
    }
} 