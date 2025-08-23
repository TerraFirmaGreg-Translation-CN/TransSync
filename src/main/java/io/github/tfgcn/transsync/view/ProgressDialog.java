package io.github.tfgcn.transsync.view;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProgressDialog extends JDialog {

    // 颜色常量定义（便于统一维护）
    private static final Color RED_BUTTON_COLOR = new Color(220, 53, 69);    // 取消按钮红色
    private static final Color GREEN_BUTTON_COLOR = new Color(40, 167, 69);  // 完成按钮绿色
    private static final Color BUTTON_TEXT_COLOR = Color.WHITE;              // 按钮文字白色

    @Getter
    private AtomicBoolean isCancelled;// 用于标记是否需要终止任务
    private boolean isCompleted;// 用于标记任务是否完成

    private JTable progressTable;
    private DefaultTableModel tableModel;
    private JProgressBar overallProgress;
    private JLabel progressLabel;
    private int totalFiles;
    @Getter
    private int completedFiles;
    private JButton cancelButton;
    private JButton completeButton;

    public ProgressDialog(Frame owner, String title, List<String> fileItems) {
        super(owner, title, true);
        this.isCancelled = new AtomicBoolean(false);
        this.isCompleted = false;
        this.totalFiles = fileItems.size();
        this.completedFiles = 0;

        // 初始化UI
        initUI(fileItems);

        // 设置对话框属性
        setSize(960, 500);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE); // 禁止关闭

        // 添加窗口关闭监听器
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleWindowClose();
            }
        });
    }

    private void initUI(List<String> fileItems) {
        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // 1. 表格模型（文件名、状态）
        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 表格不可编辑
            }
        };
        tableModel.addColumn("文件名");
        tableModel.addColumn("状态");

        // 添加文件数据
        for (String item : fileItems) {
            tableModel.addRow(new Vector<>() {{
                add(item);
                add("等待");
            }});
        }

        // 创建表格并设置滚动面板（固定高度）
        progressTable = new JTable(tableModel);
        progressTable.getColumnModel().getColumn(0).setPreferredWidth(600);
        progressTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        progressTable.setRowHeight(25);

        JScrollPane scrollPane = new JScrollPane(progressTable);
        scrollPane.setPreferredSize(new Dimension(650, 350));
        scrollPane.setBorder(BorderFactory.createTitledBorder("文件上传状态"));

        // 2. 进度条和进度文本
        JPanel progressPanel = new JPanel(new BorderLayout(10, 0));

        overallProgress = new JProgressBar(0, totalFiles);
        overallProgress.setStringPainted(false);

        progressLabel = new JLabel("0/" + totalFiles);
        progressLabel.setHorizontalAlignment(SwingConstants.CENTER);

        progressPanel.add(overallProgress, BorderLayout.CENTER);
        progressPanel.add(progressLabel, BorderLayout.EAST);

        // 取消按钮
        cancelButton = new JButton("取消上传");
        customizeButton(cancelButton, RED_BUTTON_COLOR);
        cancelButton.addActionListener(e -> handleCancel());

        // 完成按钮（初始隐藏）
        completeButton = new JButton("完成");
        customizeButton(completeButton, GREEN_BUTTON_COLOR);
        completeButton.addActionListener(e -> {
            dispose(); // 点击完成后关闭对话框
        });
        completeButton.setVisible(false);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(cancelButton);
        buttonPanel.add(completeButton);

        // 组装面板
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)), BorderLayout.SOUTH);
        mainPanel.add(progressPanel, BorderLayout.SOUTH);
        mainPanel.add(buttonPanel, BorderLayout.PAGE_END);

        setContentPane(mainPanel);
    }

    // 按钮样式定制方法
    private void customizeButton(JButton button, Color bgColor) {
        button.setBackground(bgColor);
        button.setForeground(BUTTON_TEXT_COLOR);
        button.setBorderPainted(false);  // 去除边框
        button.setOpaque(true);         // 确保背景色可见
        button.setPreferredSize(new Dimension(100, 30)); // 统一按钮大小
        // 添加鼠标悬停效果
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });
    }

    // 更新单个文件状态
    public void updateFileStatus(int index, String status) {
        tableModel.setValueAt(status, index, 1);

        // 如果状态是完成或失败，更新总体进度
        if ("完成".equals(status) || status.startsWith("失败")) {
            completedFiles++;
            overallProgress.setValue(completedFiles);
            progressLabel.setText(completedFiles + "/" + totalFiles);
        }
    }

    // 处理取消操作
    private void handleCancel() {
        // 询问用户确认
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "确定要取消上传吗？未完成的文件将停止上传。",
                "确认取消",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            // 设置取消标记
            isCancelled.set(true);
            cancelButton.setEnabled(false);
            cancelButton.setText("取消中...");
            cancelButton.setBackground(RED_BUTTON_COLOR.brighter()); // 视觉反馈
        }
    }

    // 处理窗口关闭操作（根据任务状态改变行为）
    private void handleWindowClose() {
        if (isCompleted) {
            // 任务已完成，直接关闭
            dispose();
        } else {
            // 任务进行中，当作取消操作处理
            handleCancel();
        }
    }

    // 任务结束时更新按钮状态
    public void onTaskFinished() {
        cancelButton.setVisible(false);
        completeButton.setVisible(true);
        isCompleted = true;
        // 更改对话框标题以反映最终状态
        setTitle(isCancelled.get() ? "任务已取消" : "任务完成");
    }
}