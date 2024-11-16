package com.dbdiff.plugin.ui;

import com.dbdiff.plugin.model.TableDiff;
import com.dbdiff.plugin.model.ColumnDiff;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.ui.components.JBLabel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.List;

public class DiffResultDialog extends DialogWrapper {
    private final List<TableDiff> diffs;
    private JBTable diffTable;
    private static final Color ADDED_COLOR = new Color(144, 238, 144);  // 浅绿色
    private static final Color REMOVED_COLOR = new Color(255, 182, 193); // 浅红色
    private static final Color MODIFIED_COLOR = new Color(173, 216, 230); // 浅蓝色

    public DiffResultDialog(Project project, List<TableDiff> diffs) {
        super(project);
        this.diffs = diffs;
        setTitle("Database Structure Differences");
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setPreferredSize(new Dimension(1000, 600));

        // 创建表格模型
        String[] columnNames = {"表名", "字段", "源类型", "目标类型", "可空", "注释", "差异类型"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // 填充数据
        for (TableDiff tableDiff : diffs) {
            if (tableDiff.getDiffType() == TableDiff.DiffType.ADDED) {
                model.addRow(new Object[]{
                    tableDiff.getTableName(),
                    "",
                    "",
                    "",
                    "",
                    "",
                    "新增表"
                });
            } else if (tableDiff.getDiffType() == TableDiff.DiffType.REMOVED) {
                model.addRow(new Object[]{
                    tableDiff.getTableName(),
                    "",
                    "",
                    "",
                    "",
                    "",
                    "删除表"
                });
            }

            if (tableDiff.getColumnDiffs() != null) {
                for (ColumnDiff columnDiff : tableDiff.getColumnDiffs()) {
                    model.addRow(new Object[]{
                        tableDiff.getTableName(),
                        columnDiff.getColumnName(),
                        columnDiff.getSourceType(),
                        columnDiff.getTargetType(),
                        "",  // 可空
                        "",  // 注释
                        getDiffDescription(columnDiff)
                    });
                }
            }
        }

        // 创建表格并设置渲染器
        diffTable = new JBTable(model);
        diffTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        diffTable.getTableHeader().setReorderingAllowed(false);
        diffTable.setRowHeight(30);
        
        // 设置列宽
        diffTable.getColumnModel().getColumn(0).setPreferredWidth(200); // 表名
        diffTable.getColumnModel().getColumn(1).setPreferredWidth(150); // 字段
        diffTable.getColumnModel().getColumn(2).setPreferredWidth(150); // 源类型
        diffTable.getColumnModel().getColumn(3).setPreferredWidth(150); // 目标类型
        diffTable.getColumnModel().getColumn(4).setPreferredWidth(80);  // 可空
        diffTable.getColumnModel().getColumn(5).setPreferredWidth(200); // 注释
        diffTable.getColumnModel().getColumn(6).setPreferredWidth(100); // 差异类型

        // 设置单元格渲染器
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                String diffType = (String) table.getModel().getValueAt(row, 6);
                if (!isSelected) {
                    if (diffType.contains("新增")) {
                        c.setBackground(ADDED_COLOR);
                    } else if (diffType.contains("删除")) {
                        c.setBackground(REMOVED_COLOR);
                    } else if (diffType.contains("修改")) {
                        c.setBackground(MODIFIED_COLOR);
                    } else {
                        c.setBackground(table.getBackground());
                    }
                }
                return c;
            }
        };

        // 应用渲染器到所有列
        for (int i = 0; i < diffTable.getColumnCount(); i++) {
            diffTable.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        // 创建顶部面板
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JBLabel("数据库结构差异比较结果:"));
        
        // 添加到主面板
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(new JBScrollPane(diffTable), BorderLayout.CENTER);

        // 创建底部按钮面板
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton exportButton = new JButton("导出差异");
        exportButton.addActionListener(e -> exportDiff());
        bottomPanel.add(exportButton);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        return mainPanel;
    }
    
    private String getDiffDescription(ColumnDiff columnDiff) {
        switch (columnDiff.getDiffType()) {
            case ADDED:
                return "新增字段";
            case REMOVED:
                return "删除字段";
            case TYPE_CHANGED:
                return "修改类型";
            default:
                return "";
        }
    }
    
    private void exportDiff() {
        // TODO: 实现导出功能
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{getOKAction()};
    }
} 