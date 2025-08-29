package io.github.tfgcn.transsync.gui;

import io.github.tfgcn.transsync.I18n;
import io.github.tfgcn.transsync.paratranz.model.projects.ProjectStatsDto;
import io.github.tfgcn.transsync.paratranz.model.projects.ProjectsDto;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import static io.github.tfgcn.transsync.Constants.*;

public class ProjectInfoPanel extends JPanel {
    private transient ProjectsDto project;

    // 颜色常量
    private static final Color BACKGROUND_COLOR = new Color(245, 247, 250);
    private static final Color PRIMARY_COLOR = new Color(66, 133, 244);
    private static final Color SECONDARY_COLOR = new Color(112, 117, 122);
    private static final Color ACCENT_COLOR = new Color(24, 128, 56);
    private static final Color PROGRESS_BG = new Color(240, 240, 240);
    private static final Color EMPTY_TEXT_COLOR = new Color(150, 150, 150); // 空状态文本色

    // 字体常量
    private static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 22);
    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    private static final Font VALUE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    private static final Font SMALL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    private static final Font BADGE_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);

    // 固定尺寸配置
    private static final int FIXED_WIDTH = 600;  // 固定宽度
    private static final int FIXED_HEIGHT = 340; // 固定高度
    private static final int PROGRESS_BAR_WIDTH = 250; // 统计进度条宽度（支持拉伸）

    // 核心组件引用
    private JPanel contentPanel;
    private JScrollPane scrollPane;

    public ProjectInfoPanel(ProjectsDto project) {
        this.project = project;
        setPreferredSize(new Dimension(FIXED_WIDTH, FIXED_HEIGHT)); // 固定首选尺寸
        setMaximumSize(new Dimension(FIXED_WIDTH, FIXED_HEIGHT));   // 限制最大尺寸
        setMinimumSize(new Dimension(FIXED_WIDTH, FIXED_HEIGHT));   // 限制最小尺寸
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        setBackground(BACKGROUND_COLOR);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // 初始化内容面板
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(BACKGROUND_COLOR);
        contentPanel.setBorder(new EmptyBorder(0, 0, 0, 10)); // 右侧留空避免滚动条遮挡

        // 初始化滚动面板
        scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); // 禁用水平滚动

        // 填充初始内容
        refreshContent();

        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * 动态更新项目信息的核心接口
     * @param newProject 新的项目数据（支持null）
     */
    public void updateProject(ProjectsDto newProject) {
        this.project = newProject;
        refreshContent(); // 刷新面板内容
    }

    /**
     * 刷新内容面板
     */
    void refreshContent() {
        // 清空原有内容
        contentPanel.removeAll();

        // 重新添加所有内容组件
        contentPanel.add(createGeneralInfoPanel());
        contentPanel.add(Box.createRigidArea(new Dimension(0, 15))); // 垂直间距

        // 添加分隔线
        JSeparator separator1 = new JSeparator(SwingConstants.HORIZONTAL);
        separator1.setForeground(new Color(220, 220, 220));
        contentPanel.add(separator1);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // 添加统计信息部分
        contentPanel.add(createStatsPanel());

        // 强制刷新UI
        contentPanel.revalidate();
        contentPanel.repaint();
        scrollPane.revalidate();
        scrollPane.repaint();
    }

    /**
     * 创建项目基本信息面板
     */
    private JPanel createGeneralInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 0));
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(new EmptyBorder(0, 0, 0, 0));

        // 中间信息区域
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(BACKGROUND_COLOR);

        // 1. 项目名称
        String projectName = project != null ? project.getName() : I18n.getString("label.emptyName");
        JLabel nameLabel = new JLabel(projectName);
        nameLabel.setFont(TITLE_FONT);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (project == null) {
            nameLabel.setForeground(EMPTY_TEXT_COLOR); // 空状态文本置灰
        }
        infoPanel.add(nameLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        // 2. 创建时间
        String createTime = (project != null && project.getCreatedAt() != null)
                ? I18n.getString("label.createdAt") + " " + formatDate(project.getCreatedAt())
                : I18n.getString("label.emptyDate");
        JLabel createdAtLabel = new JLabel(createTime);
        createdAtLabel.setFont(SMALL_FONT);
        createdAtLabel.setForeground(SECONDARY_COLOR);
        createdAtLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(createdAtLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        // 3. 标签区域
        JPanel badgesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        badgesPanel.setBackground(BACKGROUND_COLOR);
        badgesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 游戏标签
        String gameName = (project != null && project.getGameName() != null)
                ? project.getGameName()
                : EMPTY_BADGE;
        JLabel gameBadge = createBadge(gameName, SECONDARY_COLOR);
        badgesPanel.add(gameBadge);

        // 活跃度标签
        String activeLevel = I18n.getString("label.activityIn7Days") + " " +
                ((project != null && project.getActiveLevel() != null)
                    ? project.getActiveLevel() : I18n.getString("label.unknown"));
        JLabel activityBadge = createBadge(activeLevel, new Color(220, 53, 69));
        badgesPanel.add(activityBadge);

        // 语言方向标签
        String langDir = (project != null && project.getSource() != null && project.getDest() != null)
                ? project.getSource() + " → " + project.getDest()
                : I18n.getString("label.unknown") + " → " + I18n.getString("label.unknown");
        JLabel langBadge = createBadge(langDir, ACCENT_COLOR);
        badgesPanel.add(langBadge);

        infoPanel.add(badgesPanel);
        panel.add(infoPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建统计信息面板
     */
    private JPanel createStatsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BACKGROUND_COLOR);

        // 处理null统计数据
        ProjectStatsDto stats = (project != null) ? project.getStats() : null;

        if (stats != null) {
            // 提取统计数据
            int total = stats.getTotal();
            int translated = stats.getTranslated();
            int checked = stats.getChecked();
            int reviewed = stats.getReviewed();
            int words = stats.getWords();
            int hidden = stats.getHidden();

            double tp = stats.getTp();
            double cp = stats.getCp();
            double rp = stats.getRp();

            // 添加统计行
            panel.add(createStatRow(I18n.getString("label.totalWords"), null, formatNumber(words)));
            panel.add(Box.createRigidArea(new Dimension(0, 10)));

            panel.add(createStatRow(I18n.getString("label.totalStrings"), null, formatNumber(total) + " (+" + hidden + ")"));
            panel.add(Box.createRigidArea(new Dimension(0, 10)));

            panel.add(createStatRow(I18n.getString("label.translatedStrings"), createProgressBar(tp, PRIMARY_COLOR),
                    formatNumber(translated) + " (" + formatPercent(tp) + ")"));
            panel.add(Box.createRigidArea(new Dimension(0, 10)));

            panel.add(createStatRow(I18n.getString("label.checkedStrings"), createProgressBar(cp, new Color(0, 150, 136)),
                    formatNumber(checked) + " (" + formatPercent(cp) + ")"));
            panel.add(Box.createRigidArea(new Dimension(0, 10)));

            panel.add(createStatRow(I18n.getString("label.reviewedStrings"), createProgressBar(rp, new Color(67, 160, 71)),
                    formatNumber(reviewed) + " (" + formatPercent(rp) + ")"));
        } else {
            // 空统计状态显示
            JLabel noStatsLabel = new JLabel(I18n.getString("label.emptyStats"));
            noStatsLabel.setFont(LABEL_FONT);
            noStatsLabel.setForeground(EMPTY_TEXT_COLOR);
            noStatsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(noStatsLabel);
        }

        return panel;
    }

    /**
     * 创建统计行
     */
    private JPanel createStatRow(String label, JComponent progress, String value) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(BACKGROUND_COLOR);

        // 左侧标签（固定宽度，避免抖动）
        JLabel labelLabel = new JLabel(label);
        labelLabel.setFont(VALUE_FONT);
        labelLabel.setPreferredSize(new Dimension(120, 24)); // 固定标签宽度
        labelLabel.setMinimumSize(new Dimension(120, 24));
        panel.add(labelLabel, BorderLayout.WEST);

        // 中间区域（进度条或拉伸空间）
        if (progress != null) {
            panel.add(progress, BorderLayout.CENTER); // 进度条支持拉伸
        } else {
            panel.add(Box.createHorizontalGlue(), BorderLayout.CENTER); // 空白拉伸填充
        }

        // 右侧数值（固定宽度，右对齐）
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(VALUE_FONT);
        valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        valueLabel.setPreferredSize(new Dimension(120, 24)); // 固定数值宽度
        valueLabel.setMinimumSize(new Dimension(120, 24));
        // 空状态数值置灰
        if (project == null && value.equals(EMPTY_STAT_VALUE)) {
            valueLabel.setForeground(EMPTY_TEXT_COLOR);
        }
        panel.add(valueLabel, BorderLayout.EAST);

        return panel;
    }

    /**
     * 创建进度条（支持拉伸，固定高度）
     */
    private JPanel createProgressBar(double value, Color color) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND_COLOR);
        panel.setPreferredSize(new Dimension(PROGRESS_BAR_WIDTH, 24)); // 固定高度，宽度支持拉伸
        panel.setMinimumSize(new Dimension(150, 24)); // 最小宽度限制

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue((int) (value * 100));
        progressBar.setStringPainted(true);
        progressBar.setString(formatPercent(value));
        progressBar.setForeground(color);
        progressBar.setBackground(PROGRESS_BG);
        progressBar.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        progressBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 20)); // 进度条宽度自适应父容器

        panel.add(progressBar, BorderLayout.CENTER);
        return panel;
    }

    /**
     * 创建标签组件
     */
    private JLabel createBadge(String text, Color bgColor) {
        JLabel badge = new JLabel(text);
        badge.setFont(BADGE_FONT);
        badge.setOpaque(true);
        badge.setBackground(bgColor);
        badge.setForeground(Color.WHITE);
        // 标签边框与内边距
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 1),
                BorderFactory.createEmptyBorder(3, 7, 3, 7)
        ));
        return badge;
    }

    // ==================== 工具方法 ====================
    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return sdf.format(date);
    }

}