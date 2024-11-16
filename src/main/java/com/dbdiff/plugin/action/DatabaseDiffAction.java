package com.dbdiff.plugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.dbdiff.plugin.ui.DatabaseDiffDialog;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;

public class DatabaseDiffAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        
        // 先尝试激活已有的工具窗口
        ToolWindow toolWindow = ToolWindowManager.getInstance(project)
                .getToolWindow("Database Diff Result");
        if (toolWindow != null) {
            toolWindow.show(() -> {
                // 显示对比对话框
                DatabaseDiffDialog dialog = new DatabaseDiffDialog(project);
                dialog.show();
            });
        }
    }
} 