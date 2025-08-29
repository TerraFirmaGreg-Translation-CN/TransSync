package io.github.tfgcn.transsync.gui;

import com.formdev.flatlaf.FlatLightLaf;
import io.github.tfgcn.transsync.Config;
import io.github.tfgcn.transsync.I18n;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

import static io.github.tfgcn.transsync.Constants.*;

@Slf4j
public class MainFrame extends JFrame {
    private final transient Config config;

    JMenu fileMenu;
    JMenuItem exitItem;

    JMenu toolsMenu;
    JMenuItem uploadSourceFiles;
    JMenuItem uploadTranslatedFiles;
    JMenuItem downloadTranslatedFiles;

    JMenu helpMenu;
    JMenuItem aboutItem;

    private JTabbedPane tabbedPane;
    private DashboardPanel dashboardPanel;
    private ConfigPanel configPanel;

    public MainFrame(Config config) {
        this.config = config;

        initComponents();
        setLocalizedText();
        setupLayout();
        setupEventHandlers();
    }

    private void initComponents() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(WINDOW_MIN_WIDTH, WINDOW_MIN_HEIGHT));
        setSize(WINDOW_DEFAULT_WIDTH, WINDOW_DEFAULT_HEIGHT);
        setLocationRelativeTo(null); // 居中显示

        // 设置现代外观
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (UnsupportedLookAndFeelException e) {
            // ignore
        }

        // 创建选项卡
        tabbedPane = new JTabbedPane();

        dashboardPanel = new DashboardPanel(config);
        configPanel = new ConfigPanel(config);

        tabbedPane.addTab("仪表盘", dashboardPanel);
        tabbedPane.addTab("配置", configPanel);
        tabbedPane.addTab("日志", new LogPanel());

        // 添加菜单栏
        setJMenuBar(createMenuBar());
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // 文件菜单
        fileMenu = new JMenu("文件");
        exitItem = new JMenuItem("退出");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        // 工具菜单
        toolsMenu = new JMenu("工具");
        uploadSourceFiles = new JMenuItem("上传原文");
        uploadSourceFiles.addActionListener(e -> dashboardPanel.startUploadSources());
        toolsMenu.add(uploadSourceFiles);
        uploadTranslatedFiles = new JMenuItem("上传译文");
        uploadTranslatedFiles.addActionListener(e -> dashboardPanel.startUploadTranslations());
        toolsMenu.add(uploadTranslatedFiles);
        downloadTranslatedFiles = new JMenuItem("下载译文");
        downloadTranslatedFiles.addActionListener(e -> dashboardPanel.startDownloadTranslations());
        toolsMenu.add(downloadTranslatedFiles);

        // 帮助菜单
        helpMenu = new JMenu("帮助");
        aboutItem = new JMenuItem("关于");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(toolsMenu);
        menuBar.add(helpMenu);

        return menuBar;
    }

    private void setupEventHandlers() {
        // 配置更改监听
        configPanel.addConfigChangeListener(this::onConfigChanged);
    }

    private void onConfigChanged(Config newConfig) {
        // 更新配置并重启相关服务
        this.config.merge(newConfig);
        try {
            this.config.save();
        } catch (IOException e) {
            log.error("保存配置文件失败", e);
        }
        dashboardPanel.updateConfig(config);
    }

    private void showAboutDialog() {
        JOptionPane.showMessageDialog(this,
                I18n.getString("window.title") + " " + VERSION + "\n" + I18n.getString("window.description"),
                I18n.getString("dialog.about.title"),
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void setLocalizedText() {

        setTitle(I18n.getString("window.title"));

        fileMenu.setText(I18n.getString("menu.file"));
        exitItem.setText(I18n.getString("menu.file.exit"));
        toolsMenu.setText(I18n.getString("menu.tool"));
        uploadSourceFiles.setText(I18n.getString("menu.tool.uploadSourceFiles"));
        uploadTranslatedFiles.setText(I18n.getString("menu.tool.uploadTranslatedFiles"));
        downloadTranslatedFiles.setText(I18n.getString("menu.tool.downloadTranslatedFiles"));
        helpMenu.setText(I18n.getString("menu.help"));
        aboutItem.setText(I18n.getString("menu.help.about"));

        tabbedPane.setTitleAt(0, I18n.getString("tab.dashboard"));// "仪表盘"
        tabbedPane.setTitleAt(1, I18n.getString("tab.config"));// "配置"
        tabbedPane.setTitleAt(2, I18n.getString("tab.log"));// "日志"

    }
}