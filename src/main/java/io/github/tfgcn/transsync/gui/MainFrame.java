package io.github.tfgcn.transsync.gui;

import com.formdev.flatlaf.FlatLightLaf;
import io.github.tfgcn.transsync.Config;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

import static io.github.tfgcn.transsync.Constants.*;

@Slf4j
public class MainFrame extends JFrame {
    private final Config config;

    private JTabbedPane tabbedPane;
    private DashboardPanel dashboardPanel;
    private ConfigPanel configPanel;

    public MainFrame(Config config) {
        this.config = config;

        initComponents();
        setupLayout();
        setupEventHandlers();
    }
    
    private void initComponents() {
        setTitle(TITLE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);
        
        // 添加菜单栏
        setJMenuBar(createMenuBar());
    }
    
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // 文件菜单
        JMenu fileMenu = new JMenu("文件");
        JMenuItem exitItem = new JMenuItem("退出");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        
        // 工具菜单
        JMenu toolsMenu = new JMenu("工具");
        JMenuItem uploadOriginalFiles = new JMenuItem("上传原文");
        uploadOriginalFiles.addActionListener(e -> dashboardPanel.startUploadOriginals());
        toolsMenu.add(uploadOriginalFiles);
        JMenuItem uploadTranslatedFiles = new JMenuItem("上传译文");
        uploadTranslatedFiles.addActionListener(e -> dashboardPanel.startUploadTranslations());
        toolsMenu.add(uploadTranslatedFiles);
        JMenuItem downloadTranslatedFiles = new JMenuItem("下载译文");
        downloadTranslatedFiles.addActionListener(e -> dashboardPanel.startDownloadTranslations());
        toolsMenu.add(downloadTranslatedFiles);

        // 帮助菜单
        JMenu helpMenu = new JMenu("帮助");
        JMenuItem aboutItem = new JMenuItem("关于");
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
            TITLE + " "+ VERSION + "\n" + DESCRIPTION,
            "关于",
            JOptionPane.INFORMATION_MESSAGE);
    }
}