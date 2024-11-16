package com.dbdiff.plugin.ui;

import com.dbdiff.plugin.model.TableDiff;
import com.dbdiff.plugin.model.ColumnDiff;
import com.dbdiff.plugin.service.DatabaseDiffService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.ui.JBUI;
import com.intellij.openapi.ui.ComboBox;
import javax.swing.table.TableCellRenderer;
import com.dbdiff.plugin.service.ExportService;
import com.dbdiff.plugin.service.DatabaseConfigService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashSet;
import javax.swing.table.TableColumn;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DiffResultToolWindow implements ToolWindowFactory {
    private static final Color BACKGROUND_COLOR = new Color(32, 33, 36);
    private static final Color FOREGROUND_COLOR = new Color(188, 190, 196);
    private static final Color BUTTON_BACKGROUND = new Color(60, 63, 65);
    private static final int ROW_HEIGHT = 32;
    private static final int DIVIDER_SIZE = 1;
    
    private static final Color DIFF_HIGHLIGHT_COLOR = new Color(147, 131, 247, 50);  // 紫色半透明
    
    private static JBTable sourceTable;
    private static JBTable targetTable;
    private static DefaultTableModel sourceModel;
    private static DefaultTableModel targetModel;
    private static Project currentProject;
    private static JLabel sourceDatabaseLabel;
    private static JLabel targetDatabaseLabel;
    private static JTextArea sourceScriptArea;
    private static JTextArea targetScriptArea;

    private JButton addDatabaseButton;
    private JComboBox<String> savedConnectionsCombo;
    private static final String ADD_NEW_CONNECTION = "添加新连接...";

    private static List<TableDiff> currentDiffs;
    private static String sourceDb;
    private static String targetDb;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        currentProject = project;
        
        // 初始化表格模型和表格
        initializeTables();
        
        // 创建主面板
        JPanel mainPanel = createMainPanel();
        Content content = ContentFactory.getInstance().createContent(mainPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(32, 33, 36));

        // 创建顶部工具栏
        JPanel toolbarPanel = new JPanel(new BorderLayout());
        toolbarPanel.setBackground(new Color(32, 33, 36));
        toolbarPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 创建左侧面板（包含搜索、快速对比和耗时显示）
        JPanel toolbarLeftPanel = new JPanel();
        toolbarLeftPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 0));
        toolbarLeftPanel.setBackground(new Color(32, 33, 36));
        
        // 创建搜索面板
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBackground(new Color(32, 33, 36));
        JLabel searchLabel = new JLabel("搜索: ");
        searchLabel.setForeground(new Color(187, 187, 187));
        searchPanel.add(searchLabel, BorderLayout.WEST);
        
        // 添加搜索框
        JTextField searchField = createSearchField();
        searchField.setPreferredSize(new Dimension(200, 32));
        searchPanel.add(searchField, BorderLayout.CENTER);
        
        // 添加快速对比按钮
        JButton quickCompareButton = new JButton("快速对比");
        quickCompareButton.setPreferredSize(new Dimension(100, 32));
        quickCompareButton.addActionListener(e -> showQuickCompareDialog());
        
        // 添加耗时显示标签
        compareTimeLabel = new JLabel("");
        compareTimeLabel.setForeground(new Color(187, 187, 187));
        
        toolbarLeftPanel.add(searchPanel);
        toolbarLeftPanel.add(quickCompareButton);
        toolbarLeftPanel.add(compareTimeLabel);
        
        toolbarPanel.add(toolbarLeftPanel, BorderLayout.WEST);

        // 创建主分割面板
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setResizeWeight(0.7);
        mainSplitPane.setBorder(null);
        mainSplitPane.setDividerSize(1);
        mainSplitPane.setBackground(new Color(32, 33, 36));

        // 创建表格分割面板
        JSplitPane tablesSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        tablesSplitPane.setResizeWeight(0.5);
        tablesSplitPane.setBorder(null);
        tablesSplitPane.setDividerSize(1);
        tablesSplitPane.setBackground(new Color(32, 33, 36));
        tablesSplitPane.setDividerLocation(0.5);  // 设置分割位置为50%

        // 创建SQL脚本分割面板
        JSplitPane scriptsSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        scriptsSplitPane.setResizeWeight(0.5);
        scriptsSplitPane.setBorder(null);
        scriptsSplitPane.setDividerSize(1);
        scriptsSplitPane.setBackground(new Color(32, 33, 36));
        scriptsSplitPane.setDividerLocation(0.5);  // 设置分割位置为50%

        // 创建左右表格面板
        sourceDatabaseLabel = new JBLabel("源数据库: ");
        sourceDatabaseLabel.setForeground(new Color(187, 187, 187));
        targetDatabaseLabel = new JBLabel("目标数据库: ");
        targetDatabaseLabel.setForeground(new Color(187, 187, 187));
        
        JPanel sourceTablePanel = createTablePanel(sourceDatabaseLabel, sourceTable);
        JPanel targetTablePanel = createTablePanel(targetDatabaseLabel, targetTable);

        // 设置表格面板的最小和首选大小
        Dimension panelSize = new Dimension(400, 300);
        sourceTablePanel.setMinimumSize(panelSize);
        targetTablePanel.setMinimumSize(panelSize);
        sourceTablePanel.setPreferredSize(panelSize);
        targetTablePanel.setPreferredSize(panelSize);

        tablesSplitPane.setLeftComponent(sourceTablePanel);
        tablesSplitPane.setRightComponent(targetTablePanel);

        // 创建SQL脚本面板
        sourceScriptArea = new JTextArea();
        targetScriptArea = new JTextArea();
        
        JPanel sourceScriptPanel = createScriptPanel("源数据库缺失表的建表语句", sourceScriptArea);
        JPanel targetScriptPanel = createScriptPanel("目标数据库缺失表的建表语句", targetScriptArea);

        // 设置脚本面板的最小和首选大小
        sourceScriptPanel.setMinimumSize(panelSize);
        targetScriptPanel.setMinimumSize(panelSize);
        sourceScriptPanel.setPreferredSize(panelSize);
        targetScriptPanel.setPreferredSize(panelSize);

        scriptsSplitPane.setLeftComponent(sourceScriptPanel);
        scriptsSplitPane.setRightComponent(targetScriptPanel);

        mainSplitPane.setTopComponent(tablesSplitPane);
        mainSplitPane.setBottomComponent(scriptsSplitPane);

        // 组装主面板
        mainPanel.add(toolbarPanel, BorderLayout.NORTH);
        mainPanel.add(mainSplitPane, BorderLayout.CENTER);
        mainPanel.add(createBottomPanel(), BorderLayout.SOUTH);

        // 添加组件监听器以确保分割线对齐
        mainPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int width = mainPanel.getWidth();
                int location = width / 2;
                tablesSplitPane.setDividerLocation(location);
                scriptsSplitPane.setDividerLocation(location);
            }
        });

        return mainPanel;
    }

    private JButton createToolbarButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(32, 33, 36));
        button.setForeground(new Color(188, 190, 196));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 61, 64)),
            BorderFactory.createEmptyBorder(5, 12, 5, 12)
        ));
        button.setFocusPainted(false);
        return button;
    }

    private JPanel createConnectionToolbar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        // 添加已保存连接的下拉框
        savedConnectionsCombo = new ComboBox<>(new DefaultComboBoxModel<>());
        savedConnectionsCombo.setPreferredSize(new Dimension(200, 30));
        refreshSavedConnections();
        
        // 添加新连接按钮
        addDatabaseButton = new JButton("添加数据库连接");
        addDatabaseButton.addActionListener(e -> showAddDatabaseDialog());
        
        panel.add(new JLabel("已保存的连接: "));
        panel.add(savedConnectionsCombo);
        panel.add(addDatabaseButton);
        
        return panel;
    }

    private void refreshSavedConnections() {
        DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) savedConnectionsCombo.getModel();
        model.removeAllElements();
        
        // 从配置服务获取保存的连接
        DatabaseConfigService.State state = DatabaseConfigService.getInstance(currentProject).getState();
        if (state != null && state.savedConnections != null) {
            for (String connection : state.savedConnections) {
                model.addElement(connection);
            }
        }
        
        model.addElement(ADD_NEW_CONNECTION);
    }

    private void showAddDatabaseDialog() {
        DatabaseDiffDialog dialog = new DatabaseDiffDialog(currentProject);
        if (dialog.showAndGet()) {
            refreshSavedConnections();
        }
    }

    public static void showDiffResult(List<TableDiff> diffs, String srcDb, String tgtDb) {
        if (currentProject != null) {
            SwingUtilities.invokeLater(() -> {
                currentDiffs = diffs;
                sourceDb = srcDb;
                targetDb = tgtDb;
                
                ToolWindow toolWindow = com.intellij.openapi.wm.ToolWindowManager.getInstance(currentProject)
                        .getToolWindow("Database Diff Result");
                if (toolWindow != null) {
                    toolWindow.show(() -> {
                        sourceDatabaseLabel.setText("源数据库: " + sourceDb);
                        targetDatabaseLabel.setText("目标数据库: " + targetDb);
                        updateDiffResults(diffs);
                        updateScripts(diffs);
                    });
                }
            });
        }
    }

    private JPanel createScriptPanel(String title, JTextArea scriptArea) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(43, 43, 43));
        
        // 创建标题面板
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(60, 63, 65));
        titlePanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        
        // 左侧标题
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(new Color(187, 187, 187));
        titlePanel.add(titleLabel, BorderLayout.WEST);
        
        // 右侧复制按钮
        JButton copyButton = new JButton("复制SQL");
        copyButton.setFocusPainted(false);
        copyButton.setBackground(new Color(60, 63, 65));
        copyButton.setForeground(new Color(187, 187, 187));
        copyButton.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        copyButton.addActionListener(e -> {
            String text = scriptArea.getText();
            if (text != null && !text.trim().isEmpty()) {
                copyToClipboard(text);
            }
        });
        
        // 创建按钮容器
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.setBackground(new Color(60, 63, 65));
        buttonPanel.add(copyButton);
        titlePanel.add(buttonPanel, BorderLayout.EAST);
        
        // 配置文本区域
        scriptArea.setEditable(false);
        scriptArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        scriptArea.setBackground(new Color(43, 43, 43));
        scriptArea.setForeground(Color.WHITE);
        scriptArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // 创建滚动面板
        JBScrollPane scrollPane = new JBScrollPane(scriptArea);
        scrollPane.setBorder(null);
        scrollPane.setBackground(new Color(43, 43, 43));
        scrollPane.getViewport().setBackground(new Color(43, 43, 43));
        
        panel.add(titlePanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void copyToClipboard(String text) {
        if (text != null && !text.isEmpty()) {
            java.awt.Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new java.awt.datatransfer.StringSelection(text), null);
            Messages.showInfoMessage(currentProject, "SQL已复制到剪贴板", "复制成功");
        }
    }

    private void initializeTables() {
        // 创建表格模型，添加一列用于放置按钮
        String[] columnNames = {"表名", "差异说明", "操作"};
        sourceModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2; // 只有操作列可编辑
            }
            
            @Override
            public Class<?> getColumnClass(int column) {
                return column == 2 ? JButton.class : String.class;
            }
        };
        targetModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2;
            }
            
            @Override
            public Class<?> getColumnClass(int column) {
                return column == 2 ? JButton.class : String.class;
            }
        };

        // 创建表格
        sourceTable = new JBTable(sourceModel);
        targetTable = new JBTable(targetModel);

        // 配置表格样式
        configureTable(sourceTable);
        configureTable(targetTable);
        
        // 设置按钮列的渲染器和编辑器
        setupButtonColumn(sourceTable, 2);
        setupButtonColumn(targetTable, 2);
    }

    private void configureTable(JBTable table) {
        // 设置表格基本属性
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setRowHeight(32);
        table.getTableHeader().setReorderingAllowed(false);
        table.setBackground(new Color(32, 33, 36));
        table.setForeground(new Color(188, 190, 196));
        
        // 设置列宽
        table.getColumnModel().getColumn(0).setPreferredWidth(200); // 表名
        table.getColumnModel().getColumn(1).setPreferredWidth(150); // 差异说明
        table.getColumnModel().getColumn(2).setPreferredWidth(60);  // 操作按钮，减小宽度
    }

    private void setupButtonColumn(JBTable table, int column) {
        TableColumn buttonColumn = table.getColumnModel().getColumn(column);
        
        // 设置按钮渲染器
        buttonColumn.setCellRenderer(new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JButton button = new JButton("复制表结构");
                button.setBackground(new Color(60, 63, 65));
                button.setForeground(new Color(187, 187, 187));
                return button;
            }
        });
        
        // 设置按钮编辑器
        buttonColumn.setCellEditor(new DefaultCellEditor(new JCheckBox()) {
            private JButton button;
            
            {
                button = new JButton("复制表结构");
                button.setBackground(new Color(60, 63, 65));
                button.setForeground(new Color(187, 187, 187));
                button.addActionListener(e -> {
                    fireEditingStopped();
                    int row = table.getSelectedRow();
                    if (row >= 0) {
                        String tableName = (String) table.getValueAt(row, 0);
                        copyTableStructure(tableName, table == sourceTable);
                    }
                });
            }
            
            @Override
            public Component getTableCellEditorComponent(JTable table, Object value,
                    boolean isSelected, int row, int column) {
                return button;
            }
        });
    }

    private void copyTableStructure(String tableName, boolean isSource) {
        if (currentDiffs != null) {
            // 移除表名前的序号和点号
            String realTableName = tableName.substring(tableName.indexOf(". ") + 2);
            
            for (TableDiff diff : currentDiffs) {
                if (diff.getTableName().equals(realTableName)) {
                    String sql = diff.getCreateTableSql();
                    if (sql != null && !sql.isEmpty()) {
                        java.awt.Toolkit.getDefaultToolkit()
                            .getSystemClipboard()
                            .setContents(new java.awt.datatransfer.StringSelection(sql), null);
                        Messages.showInfoMessage(currentProject, 
                            String.format("表 %s 的结构已复制到剪贴板", realTableName), 
                            "复制成功");
                    }
                    break;
                }
            }
        }
    }

    private JPanel createTablePanel(JLabel databaseLabel, JBTable table) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(32, 33, 36));
        
        // 创建标题面板，使用 BoxLayout 来水平排列组件
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.X_AXIS));
        headerPanel.setBackground(new Color(32, 33, 36));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        
        // 数据库名称标签
        databaseLabel.setForeground(new Color(188, 190, 196));
        headerPanel.add(databaseLabel);
        
        // 添加一些水平间距
        headerPanel.add(Box.createHorizontalStrut(10));
        
        // 添加表数量标签
        JLabel tableCountLabel = new JLabel();
        tableCountLabel.setForeground(new Color(188, 190, 196));
        tableCountLabel.setName(databaseLabel == sourceDatabaseLabel ? "sourceTableCount" : "targetTableCount");
        headerPanel.add(tableCountLabel);
        
        // 标签靠左对齐，其余空间填充
        headerPanel.add(Box.createHorizontalGlue());
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // 创建表格滚动面板
        JBScrollPane scrollPane = new JBScrollPane(table);
        scrollPane.setBorder(null);
        scrollPane.setBackground(new Color(32, 33, 36));
        scrollPane.getViewport().setBackground(new Color(32, 33, 36));
        
        // 定义滚动条颜色
        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        verticalScrollBar.setBackground(new Color(32, 33, 36));
        verticalScrollBar.setForeground(new Color(60, 61, 64));
        
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private static void updateDiffResults(List<TableDiff> diffs) {
        sourceModel.setRowCount(0);
        targetModel.setRowCount(0);

        // 统计表的数量
        Set<String> sourceTables = new HashSet<>();
        Set<String> targetTables = new HashSet<>();

        // 添加序号计数器
        int sourceIndex = 1;
        int targetIndex = 1;

        for (TableDiff diff : diffs) {
            switch (diff.getDiffType()) {
                case ADDED:
                    targetTables.add(diff.getTableName());
                    addEmptyRows(sourceModel, 0);
                    targetModel.addRow(new Object[]{
                        targetIndex++ + ". " + diff.getTableName(),  // 添加序号
                        "新增",
                        "复制表结构"
                    });
                    break;
                    
                case REMOVED:
                    sourceTables.add(diff.getTableName());
                    sourceModel.addRow(new Object[]{
                        sourceIndex++ + ". " + diff.getTableName(),  // 添加序号
                        "删除",
                        "复制表结构"
                    });
                    addEmptyRows(targetModel, 0);
                    break;
                    
                case MODIFIED:
                    sourceTables.add(diff.getTableName());
                    targetTables.add(diff.getTableName());
                    if (diff.getColumnDiffs() != null && !diff.getColumnDiffs().isEmpty()) {
                        int[] changes = countChanges(diff.getColumnDiffs());
                        String changeDesc = formatChangeDescription(changes[0], changes[1], changes[2]);
                        
                        sourceModel.addRow(new Object[]{
                            sourceIndex++ + ". " + diff.getTableName(),  // 添加序号
                            changeDesc,
                            "复制表结构"
                        });
                        targetModel.addRow(new Object[]{
                            targetIndex++ + ". " + diff.getTableName(),  // 添加序号
                            changeDesc,
                            "复制表结构"
                        });
                    }
                    break;
            }
        }

        // 更新表数量显示
        SwingUtilities.invokeLater(() -> {
            // 更新源数据库表数量
            updateTableCount(sourceTable, "sourceTableCount", sourceTables.size());
            // 更新目标数据库表数量
            updateTableCount(targetTable, "targetTableCount", targetTables.size());
        });
    }

    private static int[] countChanges(List<ColumnDiff> columnDiffs) {
        int[] changes = new int[3]; // [added, removed, modified]
        for (ColumnDiff colDiff : columnDiffs) {
            switch (colDiff.getDiffType()) {
                case ADDED: changes[0]++; break;
                case REMOVED: changes[1]++; break;
                case TYPE_CHANGED: changes[2]++; break;
            }
        }
        return changes;
    }

    private static String formatChangeDescription(int added, int removed, int modified) {
        StringBuilder desc = new StringBuilder();
        if (added > 0) desc.append("+").append(added);
        if (removed > 0) desc.append(desc.length() > 0 ? " -" : "-").append(removed);
        if (modified > 0) desc.append(desc.length() > 0 ? " ~" : "~").append(modified);
        return desc.toString();
    }

    private static void updateTableCount(JBTable table, String labelName, int count) {
        Container parent = table.getParent();
        while (parent != null) {
            if (parent instanceof JPanel) {
                Component[] components = ((JPanel) parent).getComponents();
                for (Component comp : components) {
                    if (comp instanceof JPanel) {
                        for (Component innerComp : ((JPanel) comp).getComponents()) {
                            if (innerComp instanceof JLabel && labelName.equals(innerComp.getName())) {
                                ((JLabel) innerComp).setText(String.format("(共 %d 个表)", count));
                                return;
                            }
                        }
                    }
                }
            }
            parent = parent.getParent();
        }
    }

    private static void addEmptyRows(DefaultTableModel model, int count) {
        for (int i = 0; i < count; i++) {
            model.addRow(new Object[]{"", ""});
        }
    }

    private static DefaultTableCellRenderer createDiffRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (!isSelected && value != null) {
                    String cellValue = value.toString();
                    if (cellValue.contains("[新增]")) {
                        c.setBackground(new Color(144, 238, 144)); // 浅绿色
                    } else if (cellValue.contains("[删除]")) {
                        c.setBackground(new Color(255, 182, 193)); // 浅红色
                    } else if (cellValue.contains("[修改]")) {
                        c.setBackground(new Color(173, 216, 230)); // 浅蓝色
                    } else if (cellValue.startsWith("表: ")) {
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                        c.setBackground(new Color(240, 240, 240)); // 浅灰色
                    } else {
                        c.setBackground(table.getBackground());
                    }
                }
                return c;
            }
        };
    }

    private static void applyRenderer(JTable table, DefaultTableCellRenderer renderer) {
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }
    
    private void exportDiff() {
        // TODO: 实现导出功能
    }

    private static void updateScripts(List<TableDiff> diffs) {
        StringBuilder sourceScript = new StringBuilder();
        StringBuilder targetScript = new StringBuilder();

        for (TableDiff diff : diffs) {
            switch (diff.getDiffType()) {
                case ADDED:
                    // 目标数据库新增的表，源数据库缺失
                    sourceScript.append("-- 在源数据库中缺失的表: ").append(diff.getTableName()).append("\n");
                    String createSql = diff.getCreateTableSql();
                    if (!createSql.trim().endsWith(";")) {
                        createSql = createSql.trim() + ";";
                    }
                    sourceScript.append(createSql).append("\n\n");
                    break;
                case REMOVED:
                    // 源数据库有但目标数据库没有的表
                    targetScript.append("-- 在目标数据库中缺失的表: ").append(diff.getTableName()).append("\n");
                    String removedSql = diff.getCreateTableSql();
                    if (!removedSql.trim().endsWith(";")) {
                        removedSql = removedSql.trim() + ";";
                    }
                    targetScript.append(removedSql).append("\n\n");
                    break;
                case MODIFIED:
                    // 如果有列的变更，生成ALTER TABLE语句
                    if (diff.getColumnDiffs() != null && !diff.getColumnDiffs().isEmpty()) {
                        String alterTableSql = generateAlterTableSql(diff);
                        targetScript.append(alterTableSql).append("\n\n");
                    }
                    break;
            }
        }

        sourceScriptArea.setText(sourceScript.toString());
        targetScriptArea.setText(targetScript.toString());
    }

    private static String generateAlterTableSql(TableDiff diff) {
        StringBuilder sql = new StringBuilder();
        sql.append("-- 表 ").append(diff.getTableName()).append(" 的结构变更\n");
        sql.append("ALTER TABLE `").append(diff.getTableName()).append("`\n");

        List<String> alterations = new ArrayList<>();
        for (ColumnDiff columnDiff : diff.getColumnDiffs()) {
            switch (columnDiff.getDiffType()) {
                case ADDED:
                    alterations.add("ADD COLUMN `" + columnDiff.getColumnName() + "` " + 
                                  columnDiff.getTargetType());
                    break;
                case REMOVED:
                    alterations.add("DROP COLUMN `" + columnDiff.getColumnName() + "`");
                    break;
                case TYPE_CHANGED:
                    alterations.add("MODIFY COLUMN `" + columnDiff.getColumnName() + "` " + 
                                  columnDiff.getTargetType());
                    break;
            }
        }

        sql.append(String.join(",\n", alterations)).append(";");
        return sql.toString();
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton exportHtmlButton = new JButton("导出HTML报告");
        JButton exportCsvButton = new JButton("导出CSV");
        
        exportHtmlButton.addActionListener(e -> ExportService.exportToHtml(
            currentProject, currentDiffs, sourceDb, targetDb));
        exportCsvButton.addActionListener(e -> ExportService.exportToCsv(
            currentProject, currentDiffs));
        
        panel.add(exportHtmlButton);
        panel.add(exportCsvButton);
        
        return panel;
    }

    private String generateSqlScript() {
        StringBuilder sql = new StringBuilder();
        for (TableDiff diff : currentDiffs) {
            switch (diff.getDiffType()) {
                case ADDED:
                    sql.append("-- 新增表\n");
                    sql.append(diff.getCreateTableSql()).append("\n\n");
                    break;
                case REMOVED:
                    sql.append("-- 删除表\n");
                    sql.append("DROP TABLE IF EXISTS `").append(diff.getTableName()).append("`;\n\n");
                    break;
                case MODIFIED:
                    if (diff.getColumnDiffs() != null && !diff.getColumnDiffs().isEmpty()) {
                        sql.append("-- 修改表 ").append(diff.getTableName()).append("\n");
                        sql.append("ALTER TABLE `").append(diff.getTableName()).append("`\n");
                        for (ColumnDiff colDiff : diff.getColumnDiffs()) {
                            switch (colDiff.getDiffType()) {
                                case ADDED:
                                    sql.append("  ADD COLUMN `").append(colDiff.getColumnName())
                                       .append("` ").append(colDiff.getTargetType()).append(",\n");
                                    break;
                                case REMOVED:
                                    sql.append("  DROP COLUMN `").append(colDiff.getColumnName()).append("`,\n");
                                    break;
                                case TYPE_CHANGED:
                                    sql.append("  MODIFY COLUMN `").append(colDiff.getColumnName())
                                       .append("` ").append(colDiff.getTargetType()).append(",\n");
                                    break;
                            }
                        }
                        sql.append(";\n\n");
                    }
                    break;
            }
        }
        return sql.toString();
    }

    private JPanel createActionToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> refreshData());
        toolbar.add(refreshButton);
        return toolbar;
    }

    private JSplitPane createScriptsSplitPane() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.5);
        splitPane.setBorder(null);  // 移除边框
        splitPane.setDividerSize(1);  // 设置分割线宽度为1，与表格区域一致

        sourceScriptArea = new JTextArea();
        targetScriptArea = new JTextArea();
        
        JPanel sourceScriptPanel = createScriptPanel("源数据库缺失表的建表语句", sourceScriptArea);
        JPanel targetScriptPanel = createScriptPanel("目标数据库缺失表的建表语句", targetScriptArea);

        splitPane.setLeftComponent(sourceScriptPanel);
        splitPane.setRightComponent(targetScriptPanel);

        return splitPane;
    }

    private void refreshData() {
        if (currentDiffs != null) {
            updateDiffResults(currentDiffs);
            updateScripts(currentDiffs);
        }
    }

    private void showQuickCompareDialog() {
        DatabaseDiffDialog dialog = new DatabaseDiffDialog(currentProject);
        // 加载上次的配置
        DatabaseConfigService.State state = DatabaseConfigService.getInstance(currentProject).getState();
        if (state != null && !state.sourceUrl.isEmpty() && !state.targetUrl.isEmpty()) {
            try {
                long startTime = System.currentTimeMillis();
                
                // 创建数据库连接
                Connection sourceConn = createConnection(
                    state.sourceUrl,
                    state.sourceUser,
                    state.sourcePassword
                );
                
                Connection targetConn = createConnection(
                    state.targetUrl,
                    state.targetUser,
                    state.targetPassword
                );
                
                // 执行比较
                DatabaseDiffService diffService = new DatabaseDiffService();
                List<TableDiff> diffs = diffService.compareDatabase(sourceConn, targetConn);
                
                // 计算耗时
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                compareTimeLabel.setText(String.format("耗时: %d ms", duration));
                
                // 获取数据库名称
                String sourceDb = sourceConn.getCatalog();
                String targetDb = targetConn.getCatalog();
                
                // 关闭连接
                sourceConn.close();
                targetConn.close();
                
                // 显示结果
                showDiffResult(diffs, sourceDb, targetDb);
                
            } catch (Exception e) {
                // 如果出错，再显示配置对话框
                Messages.showErrorDialog(
                    currentProject,
                    "快速对比失败: " + e.getMessage() + "\n请重新配置数据库连接",
                    "连接错误"
                );
                dialog.show();
            }
        } else {
            // 如果没有保存的配置，显示配置对话框
            dialog.show();
        }
    }

    private Connection createConnection(String url, String user, String password) throws SQLException {
        // 验证并修正 URL 格式
        if (!url.toLowerCase().startsWith("jdbc:")) {
            url = "jdbc:mysql://" + url;
        }
        
        try {
            // 确保 MySQL 驱动被加载
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // 添加常用参数
            if (!url.contains("?")) {
                url += "?";
            } else if (!url.endsWith("&") && !url.endsWith("?")) {
                url += "&";
            }
            
            url += "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            
            return DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found", e);
        }
    }

    private JTextField createSearchField() {
        JTextField searchField = new JTextField(20);
        searchField.setBackground(new Color(50, 51, 54));
        searchField.setForeground(new Color(188, 190, 196));
        
        // 创建一个更大的边框
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 61, 64)),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)  // 增加内边距
        ));
        
        // 添加搜索功能
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { filterTables(); }
            public void removeUpdate(DocumentEvent e) { filterTables(); }
            public void insertUpdate(DocumentEvent e) { filterTables(); }

            private void filterTables() {
                String searchText = searchField.getText().toLowerCase();
                if (currentDiffs != null) {
                    List<TableDiff> filteredDiffs = currentDiffs.stream()
                        .filter(diff -> matchesSearch(diff, searchText))
                        .collect(Collectors.toList());
                    updateDiffResults(filteredDiffs);
                }
            }

            private boolean matchesSearch(TableDiff diff, String searchText) {
                // 匹配表名
                if (diff.getTableName().toLowerCase().contains(searchText)) {
                    return true;
                }
                
                // 匹配列名和类型
                if (diff.getColumnDiffs() != null) {
                    return diff.getColumnDiffs().stream().anyMatch(col -> 
                        col.getColumnName().toLowerCase().contains(searchText) ||
                        (col.getSourceType() != null && col.getSourceType().toLowerCase().contains(searchText)) ||
                        (col.getTargetType() != null && col.getTargetType().toLowerCase().contains(searchText))
                    );
                }
                return false;
            }
        });
        
        // 添加搜索提示
        searchField.putClientProperty("JTextField.placeholderText", "搜索表名或字段...");
        
        return searchField;
    }

    // 添加耗时标签字段
    private static JLabel compareTimeLabel;
} 