package io.github.tfgcn.transsync.gui;

import io.github.tfgcn.transsync.Config;
import io.github.tfgcn.transsync.paratranz.api.FilesApi;
import io.github.tfgcn.transsync.paratranz.api.StringsApi;
import io.github.tfgcn.transsync.paratranz.ParatranzApiFactory;
import io.github.tfgcn.transsync.paratranz.api.ProjectsApi;
import io.github.tfgcn.transsync.paratranz.model.files.FilesDto;
import io.github.tfgcn.transsync.paratranz.model.projects.ProjectsDto;
import io.github.tfgcn.transsync.service.SyncService;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static io.github.tfgcn.transsync.Constants.ENSURE_FORCE_MESSAGE;
import static io.github.tfgcn.transsync.Constants.ENSURE_FORCE_TITLE;

public class DashboardPanel extends JPanel {

    private final transient Config config;

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

        // 1. 中间核心区域 - 项目信息面板
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

        try {
            SyncService service = getSyncService();

            // 3. 获取待处理文件列表
            List<FilesDto> files = service.fetchRemoteFiles();
            if (files.isEmpty()) {
                JOptionPane.showMessageDialog(this, "没有需要下载的译文文件", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // 4. 创建并显示ProgressDialog
            ProgressDialog dialog = new ProgressDialog(
                    (Frame) SwingUtilities.getWindowAncestor(this),
                    "下载译文",
                    TaskType.DOWNLOAD_TRANSLATIONS,
                    service,
                    files,
                    null
            );
            dialog.setVisible(true);

            // 5. 任务结束后刷新UI
            loadProjectInfo();
            updateStatus();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "下载准备失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        } finally {
            enableButtons();
        }
    }

    /**
     * 上传译文逻辑
     */
    public void startUploadTranslations() {
        disableButtons();

        // 弹出对话框，确认是否需要强制上传覆盖未翻译文本
        int option = JOptionPane.showConfirmDialog(this, ENSURE_FORCE_MESSAGE, ENSURE_FORCE_TITLE,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        Boolean force = option == JOptionPane.YES_OPTION;

        try {
            SyncService service = getSyncService();

            // 3. 获取待处理文件列表
            List<FilesDto> files = service.fetchRemoteFiles();
            if (files.isEmpty()) {
                JOptionPane.showMessageDialog(this, "没有需要上传的译文文件", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // 4. 创建并显示ProgressDialog
            ProgressDialog dialog = new ProgressDialog(
                    (Frame) SwingUtilities.getWindowAncestor(this),
                    "上传译文",
                    TaskType.UPLOAD_TRANSLATIONS,
                    service,
                    files,
                    force
            );
            dialog.setVisible(true);

            // 5. 任务结束后刷新UI
            loadProjectInfo();
            updateStatus();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "上传准备失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        } finally {
            enableButtons();
        }
    }

    /**
     * 上传原文逻辑
     */
    public void startUploadOriginals() {
        try {
            disableButtons();

            // 1. 初始化SyncService并配置
            SyncService service = getSyncService();

            // 2. 获取待处理文件列表（提前检查，无文件则提示）
            List<File> files = service.getOriginalFiles();
            if (files.isEmpty()) {
                JOptionPane.showMessageDialog(this, "没有需要上传的原文文件", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // 3. 预先加载远程文件缓存（避免任务中重复加载）
            service.fetchRemoteFiles();

            // 4. 创建并显示ProgressDialog（任务逻辑交给对话框）
            ProgressDialog dialog = new ProgressDialog(
                    (Frame) SwingUtilities.getWindowAncestor(this),
                    "上传原文",
                    TaskType.UPLOAD_ORIGINALS,
                    service,
                    files,
                    null // 无需force参数
            );
            dialog.setVisible(true); // 模态显示，对话框关闭后再执行后续操作

            // 5. 任务结束后刷新UI
            loadProjectInfo();
            updateStatus();

        } catch (Exception e) {
            // 初始化失败时提示
            JOptionPane.showMessageDialog(this, "上传准备失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        } finally {
            enableButtons(); // 确保按钮恢复可用
        }
    }

    public void disableButtons() {
        uploadOriginalsButton.setEnabled(false);
        uploadTranslationsButton.setEnabled(false);
        downloadTranslationsButton.setEnabled(false);
    }

    public void enableButtons() {
        uploadOriginalsButton.setEnabled(true);
        uploadTranslationsButton.setEnabled(true);
        downloadTranslationsButton.setEnabled(true);
    }

    public void updateStatus() {
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

    public void loadProjectInfo() {
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

    /**
     * 获取一个新的 SyncService 实例
     * @return
     * @throws IOException
     */
    public SyncService getSyncService() throws IOException {
        SyncService service = new SyncService();
        service.setWorkspace(config.getWorkspace());
        service.setProjectId(config.getProjectId());

        ParatranzApiFactory factory = new ParatranzApiFactory(config);
        service.setFilesApi(factory.create(FilesApi.class));
        service.setStringsApi(factory.create(StringsApi.class));
        return service;
    }
}
