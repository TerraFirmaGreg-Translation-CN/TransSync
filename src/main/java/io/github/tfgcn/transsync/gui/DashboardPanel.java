package io.github.tfgcn.transsync.gui;

import io.github.tfgcn.transsync.Config;
import io.github.tfgcn.transsync.paratranz.api.FilesApi;
import io.github.tfgcn.transsync.paratranz.model.files.FilesDto;
import io.github.tfgcn.transsync.paratranz.ParatranzApiFactory;
import io.github.tfgcn.transsync.paratranz.api.ProjectsApi;
import io.github.tfgcn.transsync.paratranz.model.projects.ProjectsDto;
import io.github.tfgcn.transsync.service.SyncService;
import org.apache.commons.collections4.CollectionUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class DashboardPanel extends JPanel {

    private final Config config;

    private ProjectInfoPanel projectInfoPanel;
    private JLabel paratranzStatusLabel;
    private JButton uploadOriginalsButton;  // 上传同步按钮
    private JButton uploadTranslationsButton;  // 上传同步按钮
    private JButton downloadTranslationsButton; // 下载同步按钮

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
        uploadTranslationsButton.addActionListener(e -> startUploadTranslations());
        downloadTranslationsButton.addActionListener(e -> startDownloadTranslations());
    }

    /**
     * 处理批量下载译文逻辑
     */
    public void startDownloadTranslations() {
        disableButtons();

        new Thread(() -> {
            AtomicReference<ProgressDialog> progressDialog = new AtomicReference<>();

            try {
                // 执行上传同步逻辑
                SyncService service = new SyncService();
                service.setWorkspace(config.getWorkspace());// 此处会检测工作目录，如果找不到Tools-Modern项目则抛出异常
                service.setProjectId(config.getProjectId());

                ParatranzApiFactory factory = new ParatranzApiFactory(config);
                FilesApi filesApi = factory.create(FilesApi.class);
                service.setFilesApi(filesApi);

                // 扫描远程文件文件
                List<FilesDto> remoteFiles = service.fetchRemoteFiles();
                List<String> fileNames = remoteFiles.stream().map(FilesDto::getName).collect(Collectors.toList());

                // 创建并显示进度对话框
                SwingUtilities.invokeAndWait(() -> {
                    progressDialog.set(new ProgressDialog(
                            (Frame) SwingUtilities.getWindowAncestor(DashboardPanel.this),
                            "下载译文",
                            fileNames
                    ));
                });

                // 显示对话框
                SwingUtilities.invokeLater(() -> progressDialog.get().setVisible(true));

                AtomicBoolean isCancelled = progressDialog.get().getIsCancelled();

                // 读取远程文件，建立缓存
                service.fetchRemoteFiles();

                // 执行上传操作
                for (int i = 0; i < remoteFiles.size(); i++) {
                    // 检查是否已取消，如果是则跳出循环
                    if (isCancelled.get()) {
                        // 更新剩余文件状态为"已取消"
                        for (int j = i; j < fileNames.size(); j++) {
                            final int idx = j;
                            SwingUtilities.invokeLater(() ->
                                    progressDialog.get().updateFileStatus(idx, "已取消")
                            );
                        }
                        break;
                    }

                    FilesDto remoteFile = remoteFiles.get(i);
                    final int index = i;

                    // 更新文件状态为"进行中"
                    SwingUtilities.invokeLater(() -> {
                        progressDialog.get().updateFileStatus(index, "进行中");
                    });

                    try {
                        // 执行下载
                        String result = service.downloadTranslation(remoteFile);
                        // 下载成功
                        SwingUtilities.invokeLater(() ->
                                progressDialog.get().updateFileStatus(index, result)
                        );
                    } catch (Exception ex) {
                        // 下载失败
                        final String finalErrorMsg = ex.getMessage() != null ? ex.getMessage() : "未知错误";
                        SwingUtilities.invokeLater(() ->
                                progressDialog.get().updateFileStatus(index, "失败: " + finalErrorMsg)
                        );
                    }
                }
                // 同步完成后更新UI
                SwingUtilities.invokeLater(() -> {
                    progressDialog.get().onTaskFinished(); // 关闭对话框
                    enableButtons();

                    // 刷新项目信息
                    loadProjectInfo();
                    updateStatus();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    if (progressDialog.get() != null) {
                        progressDialog.get().dispose();
                    }
                    enableButtons();
                    JOptionPane.showMessageDialog(this, "下载译文失败: " + e.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    public void startUploadTranslations() {
        // 暂不支持
        JOptionPane.showMessageDialog(this, "暂不支持上传译文", "错误", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * 上传原文逻辑
     */
    public void startUploadOriginals() {
        disableButtons();

        new Thread(() -> {
            AtomicReference<ProgressDialog> progressDialog = new AtomicReference<>();

            try {
                // 执行上传同步逻辑
                SyncService service = new SyncService();
                service.setWorkspace(config.getWorkspace());// 此处会检测工作目录，如果找不到Tools-Modern项目则抛出异常
                service.setProjectId(config.getProjectId());

                ParatranzApiFactory factory = new ParatranzApiFactory(config);
                FilesApi filesApi = factory.create(FilesApi.class);
                service.setFilesApi(filesApi);

                // 扫描本地文件
                List<File> files = service.getOriginalFiles();
                if (CollectionUtils.isEmpty(files)) {
                    SwingUtilities.invokeLater(() -> {
                        enableButtons();
                        JOptionPane.showMessageDialog(this, "没有需要上传的文件", "提示", JOptionPane.INFORMATION_MESSAGE);
                    });
                    return;
                }

                List<String> fileNames = service.getRemoteFilePaths(files);

                // 创建并显示进度对话框
                SwingUtilities.invokeAndWait(() -> {
                    progressDialog.set(new ProgressDialog(
                            (Frame) SwingUtilities.getWindowAncestor(DashboardPanel.this),
                            "上传原文",
                            fileNames
                    ));
                });

                // 显示对话框
                SwingUtilities.invokeLater(() -> progressDialog.get().setVisible(true));

                AtomicBoolean isCancelled = progressDialog.get().getIsCancelled();

                // 读取远程文件，建立缓存
                service.fetchRemoteFiles();

                // 执行上传操作
                for (int i = 0; i < files.size(); i++) {
                    // 检查是否已取消，如果是则跳出循环
                    if (isCancelled.get()) {
                        // 更新剩余文件状态为"已取消"
                        for (int j = i; j < fileNames.size(); j++) {
                            final int idx = j;
                            SwingUtilities.invokeLater(() ->
                                    progressDialog.get().updateFileStatus(idx, "已取消")
                            );
                        }
                        break;
                    }
                    File file = files.get(i);
                    final int index = i;

                    // 更新文件状态为"进行中"
                    SwingUtilities.invokeLater(() -> {
                        progressDialog.get().updateFileStatus(index, "进行中");
                    });

                    try {
                        // 执行上传
                        String result = service.uploadOriginalFile(file);
                        // 上传成功
                        SwingUtilities.invokeLater(() ->
                                progressDialog.get().updateFileStatus(index, result)
                        );
                    } catch (Exception ex) {
                        // 上传失败
                        final String finalErrorMsg = ex.getMessage() != null ? ex.getMessage() : "未知错误";
                        SwingUtilities.invokeLater(() ->
                                progressDialog.get().updateFileStatus(index, "失败: " + finalErrorMsg)
                        );
                    }
                }
                // 同步完成后更新UI
                SwingUtilities.invokeLater(() -> {
                    progressDialog.get().onTaskFinished(); // 关闭对话框
                    enableButtons();

                    // 刷新项目信息
                    loadProjectInfo();
                    updateStatus();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    if (progressDialog.get() != null) {
                        progressDialog.get().dispose();
                    }
                    enableButtons();
                    JOptionPane.showMessageDialog(this, "上传原文失败: " + e.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
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
                boolean paratranzConnected = config.checkParatranzConnected();
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
                    ProjectsApi projectsApi = new ParatranzApiFactory(config).create(ProjectsApi.class);
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
