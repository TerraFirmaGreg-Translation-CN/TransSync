package io.github.tfgcn.transsync.view;

import io.github.tfgcn.transsync.Config;
import io.github.tfgcn.transsync.service.SyncService;
import io.github.tfgcn.transsync.paratranz.ParatranzApiFactory;
import io.github.tfgcn.transsync.paratranz.api.ProjectsApi;
import io.github.tfgcn.transsync.paratranz.model.projects.ProjectsDto;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

public class DashboardPanel extends JPanel {
    private final Config config;
    private ProjectsApi projectsApi;

    private ProjectInfoPanel projectInfoPanel;
    private JLabel paratranzStatusLabel;
    private JButton uploadOriginalsButton;  // 上传同步按钮
    private JButton uploadTranslationsButton;  // 上传同步按钮
    private JButton downloadTranslationsButton; // 下载同步按钮
    private JProgressBar progressBar;

    public DashboardPanel(Config config) {
        this.config = config;

        initComponents();
        setupLayout();
        setupEventHandlers();
        updateStatus();
        loadProjectInfo();
    }

    private void initComponents() {
        // 初始化项目信息面板
        projectInfoPanel = new ProjectInfoPanel(null);

        // 状态和控制组件
        paratranzStatusLabel = new JLabel("检查中...");

        // 同步按钮
        uploadOriginalsButton = new JButton("上传原文");
        uploadTranslationsButton = new JButton("上传译文");
        downloadTranslationsButton = new JButton("下载译文");

        progressBar = new JProgressBar();
        progressBar.setVisible(false);

        // 统一按钮大小
        Dimension buttonSize = new Dimension(120, 25);
        uploadOriginalsButton.setPreferredSize(buttonSize);
        uploadTranslationsButton.setPreferredSize(buttonSize);
        downloadTranslationsButton.setPreferredSize(buttonSize);
    }

    private void setupLayout() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;

        // 1. 中间核心区域 - 项目信息面板（突出显示）
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0; // 垂直权重让中间区域占据主要空间
        gbc.insets = new Insets(15, 10, 15, 10);
        add(projectInfoPanel, gbc);

        // 2. 底部操作区 - 两个同步按钮和进度条
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        controlPanel.add(uploadOriginalsButton);
        controlPanel.add(uploadTranslationsButton);
        controlPanel.add(downloadTranslationsButton);
        controlPanel.add(progressBar);

        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.SOUTH;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 10, 5, 10);
        add(controlPanel, gbc);

        // 3. 最底部状态栏
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEtchedBorder());

        // 状态栏左侧区域（包含最后同步时间和Paratranz状态）
        JPanel statusLeftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        statusLeftPanel.add(new JLabel("Paratranz 状态: "));
        statusLeftPanel.add(paratranzStatusLabel);
        statusBar.add(statusLeftPanel, BorderLayout.WEST);

        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.SOUTH;
        gbc.insets = new Insets(0, 0, 0, 0); // 状态栏无额外边距
        add(statusBar, gbc);
    }

    private void setupEventHandlers() {
        uploadOriginalsButton.addActionListener(e -> startUploadOriginals());
        downloadTranslationsButton.addActionListener(e -> startDownloadSync());
    }

    /**
     * 处理上传英语文件逻辑
     */
    private void handleUploadEnglishFile(ActionEvent e) {
        JOptionPane.showMessageDialog(this, "上传英语文件功能将在这里实现", "功能提示", JOptionPane.INFORMATION_MESSAGE);
        // 实际实现时可以打开文件选择器，然后调用相应的服务类进行上传
    }

    /**
     * 处理批量下载译文逻辑
     */
    private void handleDownloadTranslations(ActionEvent e) {
        JOptionPane.showMessageDialog(this, "批量下载译文功能将在这里实现", "功能提示", JOptionPane.INFORMATION_MESSAGE);
        // 实际实现时可以打开目录选择器，然后调用相应的服务类进行下载
    }

    /**
     * 上传原文逻辑
     */
    private void startUploadOriginals() {
        disableButtons();
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        new Thread(() -> {
            try {
                // 执行上传同步逻辑
                Thread.sleep(2000L);

                SyncService syncService = new SyncService();
                //syncService.uploadSync(); // 假设存在此方法

                // 同步完成后更新UI
                SwingUtilities.invokeLater(() -> {
                    progressBar.setVisible(false);
                    enableButtons();
                    JOptionPane.showMessageDialog(this, "上传同步完成", "成功", JOptionPane.INFORMATION_MESSAGE);

                    // 同步后刷新项目信息和状态
                    loadProjectInfo();
                    updateStatus();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setVisible(false);
                    enableButtons();
                    JOptionPane.showMessageDialog(this, "上传同步失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    /**
     * 下载同步逻辑
     */
    private void startDownloadSync() {
        disableButtons();
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        new Thread(() -> {
            try {
                // 执行上传同步逻辑
                Thread.sleep(2000L);
                // 执行下载同步逻辑
                SyncService syncService = new SyncService();
                // syncService.downloadSync(); // 假设存在此方法

                // 同步完成后更新UI
                SwingUtilities.invokeLater(() -> {
                    progressBar.setVisible(false);
                    enableButtons();
                    JOptionPane.showMessageDialog(this, "下载同步完成", "成功", JOptionPane.INFORMATION_MESSAGE);

                    // 同步后刷新项目信息和状态
                    loadProjectInfo();
                    updateStatus();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setVisible(false);
                    enableButtons();
                    JOptionPane.showMessageDialog(this, "下载同步失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void disableButtons() {
        uploadOriginalsButton.setEnabled(false);
        uploadTranslationsButton.setEnabled(false);
        downloadTranslationsButton.setEnabled(false);
    }

    private void enableButtons() {
        uploadOriginalsButton.setEnabled(true);
        uploadTranslationsButton.setEnabled(true);
        downloadTranslationsButton.setEnabled(true);
    }

    private void updateStatus() {
        // 检查Paratranz连接状态
        new Thread(() -> {
            try {
                SyncService syncService = new SyncService();
                boolean paratranzConnected = syncService.checkParatranzConnected();
                SwingUtilities.invokeLater(() ->
                        paratranzStatusLabel.setText(paratranzConnected ? "已连接" : "连接失败"));
            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->
                        paratranzStatusLabel.setText("连接错误: " + e.getMessage()));
            }
        }).start();
    }

    private void loadProjectInfo() {
        // 加载项目信息并更新面板
        new Thread(() -> {
            try {
                if (config.getProjectId() != null) {
                    this.projectsApi = new ParatranzApiFactory(config).create(ProjectsApi.class);

                    ProjectsDto project = projectsApi.getProject(config.getProjectId()).execute().body();
                    SwingUtilities.invokeLater(() -> projectInfoPanel.updateProject(project));
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() ->
                        projectInfoPanel.updateProject(null));
            }
        }).start();
    }

    public void updateConfig(Config config) {
        this.config.merge(config);
        updateStatus();
        loadProjectInfo(); // 配置更新后重新加载项目信息
    }
}
