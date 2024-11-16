package com.dbdiff.plugin.service;

import com.dbdiff.plugin.model.TableDiff;
import com.dbdiff.plugin.model.ColumnDiff;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVFormat;

import java.io.*;
import java.nio.file.Files;
import java.util.List;

public class ExportService {
    
    public static void exportToHtml(Project project, List<TableDiff> diffs, String sourceDb, String targetDb) {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false);
        VirtualFile file = FileChooser.chooseFile(descriptor, project, null);
        if (file != null) {
            String path = file.getPath() + "/db_diff_report.html";
            try (PrintWriter writer = new PrintWriter(new FileWriter(path))) {
                writer.println("<!DOCTYPE html>");
                writer.println("<html><head>");
                writer.println("<meta charset='UTF-8'>");
                writer.println("<style>");
                writer.println("body { font-family: Arial, sans-serif; }");
                writer.println("table { border-collapse: collapse; width: 100%; }");
                writer.println("th, td { border: 1px solid #ddd; padding: 8px; }");
                writer.println("th { background-color: #f5f5f5; }");
                writer.println(".added { background-color: #90EE90; }");
                writer.println(".removed { background-color: #FFB6C1; }");
                writer.println(".modified { background-color: #ADD8E6; }");
                writer.println("</style></head><body>");
                
                writer.println("<h1>数据库结构差异报告</h1>");
                writer.println("<p>源数据库: " + sourceDb + "</p>");
                writer.println("<p>目标数据库: " + targetDb + "</p>");
                
                writer.println("<h2>差异摘要</h2>");
                writeSummary(writer, diffs);
                
                writer.println("<h2>详细差异</h2>");
                writeDetails(writer, diffs);
                
                writer.println("<h2>SQL同步脚本</h2>");
                writeSqlScripts(writer, diffs);
                
                writer.println("</body></html>");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void exportToCsv(Project project, List<TableDiff> diffs) {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false);
        VirtualFile file = FileChooser.chooseFile(descriptor, project, null);
        if (file != null) {
            String path = file.getPath() + "/db_diff_report.csv";
            try (CSVPrinter printer = new CSVPrinter(new FileWriter(path), CSVFormat.DEFAULT)) {
                printer.printRecord("表名", "操作类型", "字段名", "源类型", "目标类型", "差异描述");
                
                for (TableDiff diff : diffs) {
                    if (diff.getDiffType() == TableDiff.DiffType.ADDED) {
                        printer.printRecord(diff.getTableName(), "新增表", "", "", "", "新增表");
                    } else if (diff.getDiffType() == TableDiff.DiffType.REMOVED) {
                        printer.printRecord(diff.getTableName(), "删除表", "", "", "", "删除表");
                    }
                    
                    if (diff.getColumnDiffs() != null) {
                        for (ColumnDiff colDiff : diff.getColumnDiffs()) {
                            printer.printRecord(
                                diff.getTableName(),
                                getDiffTypeDesc(colDiff.getDiffType()),
                                colDiff.getColumnName(),
                                colDiff.getSourceType(),
                                colDiff.getTargetType(),
                                getColumnDiffDesc(colDiff)
                            );
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private static void writeSummary(PrintWriter writer, List<TableDiff> diffs) {
        int addedTables = 0, removedTables = 0, modifiedTables = 0;
        int addedColumns = 0, removedColumns = 0, modifiedColumns = 0;
        
        for (TableDiff diff : diffs) {
            switch (diff.getDiffType()) {
                case ADDED: addedTables++; break;
                case REMOVED: removedTables++; break;
                case MODIFIED: 
                    modifiedTables++;
                    if (diff.getColumnDiffs() != null) {
                        for (ColumnDiff colDiff : diff.getColumnDiffs()) {
                            switch (colDiff.getDiffType()) {
                                case ADDED: addedColumns++; break;
                                case REMOVED: removedColumns++; break;
                                case TYPE_CHANGED: modifiedColumns++; break;
                            }
                        }
                    }
                    break;
            }
        }
        
        writer.println("<table>");
        writer.println("<tr><th colspan='2'>差异统计</th></tr>");
        writer.println("<tr><td>新增表</td><td>" + addedTables + "</td></tr>");
        writer.println("<tr><td>删除表</td><td>" + removedTables + "</td></tr>");
        writer.println("<tr><td>修改表</td><td>" + modifiedTables + "</td></tr>");
        writer.println("<tr><td>新增字段</td><td>" + addedColumns + "</td></tr>");
        writer.println("<tr><td>删除字段</td><td>" + removedColumns + "</td></tr>");
        writer.println("<tr><td>修改字段</td><td>" + modifiedColumns + "</td></tr>");
        writer.println("</table>");
    }
    
    private static void writeDetails(PrintWriter writer, List<TableDiff> diffs) {
        writer.println("<table>");
        writer.println("<tr><th>表名</th><th>操作类型</th><th>字段名</th><th>源类型</th><th>目标类型</th><th>差异描述</th></tr>");
        
        for (TableDiff diff : diffs) {
            String cssClass = getCssClass(diff.getDiffType());
            
            if (diff.getDiffType() == TableDiff.DiffType.ADDED || diff.getDiffType() == TableDiff.DiffType.REMOVED) {
                writer.println(String.format(
                    "<tr class='%s'><td>%s</td><td>%s</td><td colspan='4'>%s</td></tr>",
                    cssClass,
                    diff.getTableName(),
                    getDiffTypeDesc(diff.getDiffType()),
                    diff.getDiffType() == TableDiff.DiffType.ADDED ? "新增表" : "删除表"
                ));
            }
            
            if (diff.getColumnDiffs() != null) {
                for (ColumnDiff colDiff : diff.getColumnDiffs()) {
                    writer.println(String.format(
                        "<tr class='%s'><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>",
                        getCssClass(colDiff.getDiffType()),
                        diff.getTableName(),
                        getDiffTypeDesc(colDiff.getDiffType()),
                        colDiff.getColumnName(),
                        colDiff.getSourceType(),
                        colDiff.getTargetType(),
                        getColumnDiffDesc(colDiff)
                    ));
                }
            }
        }
        writer.println("</table>");
    }
    
    private static void writeSqlScripts(PrintWriter writer, List<TableDiff> diffs) {
        writer.println("<pre>");
        for (TableDiff diff : diffs) {
            switch (diff.getDiffType()) {
                case ADDED:
                    writer.println("-- 新增表");
                    writer.println(diff.getCreateTableSql());
                    writer.println();
                    break;
                case REMOVED:
                    writer.println("-- 删除表");
                    writer.println("DROP TABLE IF EXISTS `" + diff.getTableName() + "`;");
                    writer.println();
                    break;
                case MODIFIED:
                    if (diff.getColumnDiffs() != null && !diff.getColumnDiffs().isEmpty()) {
                        writer.println("-- 修改表 " + diff.getTableName());
                        writer.println("ALTER TABLE `" + diff.getTableName() + "`");
                        for (ColumnDiff colDiff : diff.getColumnDiffs()) {
                            switch (colDiff.getDiffType()) {
                                case ADDED:
                                    writer.println("  ADD COLUMN `" + colDiff.getColumnName() + "` " + colDiff.getTargetType() + ",");
                                    break;
                                case REMOVED:
                                    writer.println("  DROP COLUMN `" + colDiff.getColumnName() + "`,");
                                    break;
                                case TYPE_CHANGED:
                                    writer.println("  MODIFY COLUMN `" + colDiff.getColumnName() + "` " + colDiff.getTargetType() + ",");
                                    break;
                            }
                        }
                        writer.println(";");
                        writer.println();
                    }
                    break;
            }
        }
        writer.println("</pre>");
    }
    
    private static String getCssClass(TableDiff.DiffType type) {
        switch (type) {
            case ADDED: return "added";
            case REMOVED: return "removed";
            case MODIFIED: return "modified";
            default: return "";
        }
    }
    
    private static String getCssClass(ColumnDiff.DiffType type) {
        switch (type) {
            case ADDED: return "added";
            case REMOVED: return "removed";
            case TYPE_CHANGED: return "modified";
            default: return "";
        }
    }
    
    private static String getDiffTypeDesc(TableDiff.DiffType type) {
        switch (type) {
            case ADDED: return "新增";
            case REMOVED: return "删除";
            case MODIFIED: return "修改";
            default: return "";
        }
    }
    
    private static String getDiffTypeDesc(ColumnDiff.DiffType type) {
        switch (type) {
            case ADDED: return "新增字段";
            case REMOVED: return "删除字段";
            case TYPE_CHANGED: return "修改字段";
            default: return "";
        }
    }
    
    private static String getColumnDiffDesc(ColumnDiff diff) {
        switch (diff.getDiffType()) {
            case ADDED:
                return "新增字段: " + diff.getTargetType();
            case REMOVED:
                return "删除字段: " + diff.getSourceType();
            case TYPE_CHANGED:
                return "类型变更: " + diff.getSourceType() + " -> " + diff.getTargetType();
            default:
                return "";
        }
    }
} 