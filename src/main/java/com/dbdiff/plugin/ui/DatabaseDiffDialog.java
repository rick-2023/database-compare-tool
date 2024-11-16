package com.dbdiff.plugin.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.JBPasswordField;
import com.dbdiff.plugin.service.DatabaseDiffService;
import com.dbdiff.plugin.service.DatabaseConfigService;
import com.dbdiff.plugin.model.TableDiff;
import com.intellij.database.dataSource.DatabaseConnection;
import com.intellij.database.dataSource.DatabaseConnectionPoint;
import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.database.psi.DbDataSource;
import com.intellij.database.psi.DbPsiFacade;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class DatabaseDiffDialog extends DialogWrapper {
    private final Project project;
    private JComboBox<DbDataSource> sourceDbCombo;
    private JComboBox<DbDataSource> targetDbCombo;
    private final JBTextField sourceUserField = new JBTextField();
    private final JBPasswordField sourcePasswordField = new JBPasswordField();
    private final JBTextField targetUserField = new JBTextField();
    private final JBPasswordField targetPasswordField = new JBPasswordField();
    private final JBTextField sourceUrlField;
    private final JBTextField targetUrlField;

    private static final String MYSQL_URL_TEMPLATE = "jdbc:mysql://localhost:3306/database_name";

    public DatabaseDiffDialog(Project project) {
        super(project);
        this.project = project;
        
        // 初始化带有占位符的文本框
        sourceUrlField = new JBTextField(MYSQL_URL_TEMPLATE);
        targetUrlField = new JBTextField(MYSQL_URL_TEMPLATE);
        
        setTitle("Database Structure Diff");
        loadConfig();
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        // 初始化数据源下拉框
        initDatabaseComboBoxes();

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(JBUI.Borders.empty(10));

        // 创建数据库连接配置面板
        JPanel connectionPanel = new JPanel(new GridBagLayout());
        connectionPanel.setBorder(BorderFactory.createTitledBorder("Database Connections"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(5);

        // 源数据库面板
        JPanel sourcePanel = createDatabasePanel(
            "Source Database",
            sourceUrlField,
            sourceUserField,
            sourcePasswordField,
            e -> testConnection(true),
            sourceDbCombo
        );

        // 目标数据库面板
        JPanel targetPanel = createDatabasePanel(
            "Target Database",
            targetUrlField,
            targetUserField,
            targetPasswordField,
            e -> testConnection(false),
            targetDbCombo
        );

        // 添加源数据库和目标数据库面板
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        connectionPanel.add(sourcePanel, gbc);

        gbc.gridy = 1;
        gbc.insets = JBUI.insets(15, 5, 5, 5);  // 增加上边距
        connectionPanel.add(targetPanel, gbc);

        // 添加提示信息面板
        JPanel tipsPanel = createTipsPanel();
        
        // 将所有面板添加到主面板
        mainPanel.add(connectionPanel, BorderLayout.CENTER);
        mainPanel.add(tipsPanel, BorderLayout.SOUTH);

        // 设置首选大小
        mainPanel.setPreferredSize(new Dimension(500, 400));
        
        return mainPanel;
    }

    private JPanel createDatabasePanel(String title, JTextField urlField, JTextField userField, 
                                     JPasswordField passwordField, ActionListener testAction,
                                     JComboBox<DbDataSource> dbCombo) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(5);
        gbc.anchor = GridBagConstraints.WEST;

        // 添加数据源选择下拉框
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Data Source:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        panel.add(dbCombo, gbc);

        // URL 行
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(new JLabel("URL:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        panel.add(urlField, gbc);

        // Username 行
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(userField, gbc);

        // Password 行
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(passwordField, gbc);

        // Test Connection 按钮
        JButton testButton = new JButton("Test Connection");
        testButton.addActionListener(testAction);
        
        gbc.gridy = 4;
        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(testButton, gbc);

        return panel;
    }

    private JPanel createTipsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Tips"));
        
        JTextArea tipsArea = new JTextArea(
            "• URL format: jdbc:mysql://localhost:3306/database_name\n" +
            "• Make sure the database user has sufficient privileges\n" +
            "• Click 'Test Connection' to verify the connection before comparing"
        );
        tipsArea.setEditable(false);
        tipsArea.setBackground(null);
        tipsArea.setFont(UIManager.getFont("Label.font"));
        
        panel.add(tipsArea, BorderLayout.CENTER);
        return panel;
    }

    private void testConnection(boolean isSource) {
        try {
            String url = isSource ? sourceUrlField.getText() : targetUrlField.getText();
            String user = isSource ? sourceUserField.getText() : targetUserField.getText();
            String password = isSource ? new String(sourcePasswordField.getPassword()) 
                                     : new String(targetPasswordField.getPassword());

            Connection conn = createConnection(url, user, password);
            conn.close();
            
            Messages.showInfoMessage(
                project,
                "Connection successful!",
                isSource ? "Source Database Connection" : "Target Database Connection"
            );
        } catch (Exception e) {
            Messages.showErrorDialog(
                project,
                "Connection failed: " + e.getMessage(),
                isSource ? "Source Database Connection Error" : "Target Database Connection Error"
            );
        }
    }

    @Override
    protected void doOKAction() {
        if (validateInput()) {
            try {
                saveConfig();
                
                Connection sourceConn = createConnection(
                    sourceUrlField.getText(),
                    sourceUserField.getText(),
                    new String(sourcePasswordField.getPassword())
                );
                
                Connection targetConn = createConnection(
                    targetUrlField.getText(),
                    targetUserField.getText(),
                    new String(targetPasswordField.getPassword())
                );
                
                DatabaseDiffService diffService = new DatabaseDiffService();
                List<TableDiff> diffs = diffService.compareDatabase(sourceConn, targetConn);
                
                // 获取数据库名称
                String sourceDb = sourceConn.getCatalog();
                String targetDb = targetConn.getCatalog();
                
                // 关闭连接
                sourceConn.close();
                targetConn.close();
                
                // 关闭当前对话框
                close(OK_EXIT_CODE);
                
                // 在工具窗口中显示结果，包含数据库名称
                DiffResultToolWindow.showDiffResult(diffs, sourceDb, targetDb);
                
            } catch (Exception e) {
                Messages.showErrorDialog(
                    project,
                    "Error: " + e.getMessage(),
                    "Database Connection Error"
                );
            }
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

    private boolean validateInput() {
        if (sourceUrlField.getText().trim().isEmpty()) {
            showError("Source URL is required");
            return false;
        }
        if (targetUrlField.getText().trim().isEmpty()) {
            showError("Target URL is required");
            return false;
        }
        
        // 验证 URL 格式
        if (!validateUrl(sourceUrlField.getText())) {
            showError("Invalid source URL format. Expected format: " + MYSQL_URL_TEMPLATE);
            return false;
        }
        if (!validateUrl(targetUrlField.getText())) {
            showError("Invalid target URL format. Expected format: " + MYSQL_URL_TEMPLATE);
            return false;
        }
        
        return true;
    }
    
    private boolean validateUrl(String url) {
        String urlLower = url.toLowerCase();
        return urlLower.startsWith("jdbc:") || urlLower.contains("://") || urlLower.contains(":");
    }

    private void showError(String message) {
        Messages.showErrorDialog(
            project,
            message,
            "Validation Error"
        );
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return sourceUrlField;
    }

    private void loadConfig() {
        DatabaseConfigService.State state = DatabaseConfigService.getInstance(project).getState();
        if (state != null) {
            sourceUrlField.setText(state.sourceUrl);
            sourceUserField.setText(state.sourceUser);
            sourcePasswordField.setText(state.sourcePassword);
            targetUrlField.setText(state.targetUrl);
            targetUserField.setText(state.targetUser);
            targetPasswordField.setText(state.targetPassword);
        }
    }

    private void saveConfig() {
        DatabaseConfigService.State state = new DatabaseConfigService.State();
        state.sourceUrl = sourceUrlField.getText();
        state.sourceUser = sourceUserField.getText();
        state.sourcePassword = new String(sourcePasswordField.getPassword());
        state.targetUrl = targetUrlField.getText();
        state.targetUser = targetUserField.getText();
        state.targetPassword = new String(targetPasswordField.getPassword());
        
        DatabaseConfigService.getInstance(project).loadState(state);
    }

    private void initDatabaseComboBoxes() {
        // 获取所有配置的数据源
        DbPsiFacade dbPsiFacade = DbPsiFacade.getInstance(project);
        List<DbDataSource> dataSources = dbPsiFacade.getDataSources();
        
        // 创建数据源选择下拉框
        DefaultComboBoxModel<DbDataSource> sourceModel = new DefaultComboBoxModel<>();
        DefaultComboBoxModel<DbDataSource> targetModel = new DefaultComboBoxModel<>();
        
        // 添加数据源到模型
        for (DbDataSource dataSource : dataSources) {
            sourceModel.addElement(dataSource);
            targetModel.addElement(dataSource);
        }
        
        sourceDbCombo = new ComboBox<>(sourceModel);
        targetDbCombo = new ComboBox<>(targetModel);

        // 设置渲染器
        ColoredListCellRenderer<DbDataSource> renderer = new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends DbDataSource> list,
                                               DbDataSource value,
                                               int index,
                                               boolean selected,
                                               boolean hasFocus) {
                if (value != null) {
                    append(value.getName());
                }
            }
        };

        sourceDbCombo.setRenderer(renderer);
        targetDbCombo.setRenderer(renderer);

        // 添加选择监听器
        sourceDbCombo.addActionListener(e -> updateConnectionFields(sourceDbCombo, 
            sourceUrlField, sourceUserField, sourcePasswordField));
        targetDbCombo.addActionListener(e -> updateConnectionFields(targetDbCombo, 
            targetUrlField, targetUserField, targetPasswordField));
    }

    private void updateConnectionFields(JComboBox<DbDataSource> comboBox,
                                      JTextField urlField,
                                      JTextField userField,
                                      JPasswordField passwordField) {
        DbDataSource selectedSource = comboBox.getSelectedItem() instanceof DbDataSource ? 
            (DbDataSource) comboBox.getSelectedItem() : null;
        
        if (selectedSource != null) {
            try {
                // 获取数据源的连接配置
                LocalDataSource dataSource = (LocalDataSource) selectedSource.getDelegate();
                
                // 使用正确的方法获取连接信息
                String url = dataSource.getUrl();
                String username = dataSource.getUsername();
                
                // 更新界面字段
                urlField.setText(url != null ? url : "");
                userField.setText(username != null ? username : "");
                
                // 密码可能无法直接获取，保持为空
                passwordField.setText("");
                
            } catch (Exception e) {
                // 如果获取配置失败，清空字段
                urlField.setText("");
                userField.setText("");
                passwordField.setText("");
                
                // 显示错误信息
                Messages.showErrorDialog(
                    project,
                    "Failed to get database connection details: " + e.getMessage(),
                    "Configuration Error"
                );
            }
        }
    }
} 