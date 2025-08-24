package io.github.tfgcn.transsync.view;

import lombok.Getter;

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

    // 颜色常量定义
    private static final Color RED_BUTTON_COLOR = new Color(220, 53, 69);    // 取消按钮红色
    private static final Color GREEN_BUTTON_COLOR = new Color(40, 167, 69);  // 完成按钮绿色
    private static final Color BUTTON_TEXT_COLOR = Color.WHITE;              // 按钮文字白色

    @Getter
    private final AtomicBoolean isCancelled;// 用于标记是否需要终止任务
    private boolean isCompleted;// 用于标记任务是否完成

    private JTable progressTable;
    private DefaultTableModel tableModel;
    private JProgressBar overallProgress;
    private final int totalFiles;
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
        // 创建主面板（BorderLayout）
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15)); // 主面板内边距

        // 1. 表格区域（文件上传状态）
        // 表格模型（文件名、状态）
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

        // 创建表格并设置滚动面板
        progressTable = new JTable(tableModel);
        progressTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        progressTable.getColumnModel().getColumn(0).setMinWidth(600);
        progressTable.getColumnModel().getColumn(1).setMinWidth(150);
        progressTable.setRowHeight(25);

        JScrollPane scrollPane = new JScrollPane(progressTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("文件状态"));
        mainPanel.add(scrollPane, BorderLayout.CENTER); // 表格占满中间区域


        // 2. 底部区域（进度条 + 按钮）：核心修复！用垂直BoxLayout整合
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS)); // 垂直排列
        bottomPanel.setBorder(new EmptyBorder(10, 0, 0, 0)); // 顶部间距（与表格分隔）


        // 2.1 进度条面板（包含进度条和计数文本）
        JPanel progressPanel = new JPanel(new BorderLayout(10, 0));
        progressPanel.setBorder(BorderFactory.createTitledBorder("进度")); // 进度条标题边框

        // 进度条优化：启用百分比文本，设置高度
        overallProgress = new JProgressBar(0, totalFiles);
        overallProgress.setStringPainted(true); // 显示百分比（如 "50%"）
        overallProgress.setPreferredSize(new Dimension(0, 20)); // 固定进度条高度，宽度自适应

        progressPanel.add(overallProgress, BorderLayout.CENTER);
        bottomPanel.add(progressPanel); // 进度条添加到底部容器


        // 2.2 按钮面板（取消/完成按钮）
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(new EmptyBorder(10, 0, 0, 0)); // 顶部间距（与进度条分隔）

        // 取消按钮
        cancelButton = new JButton("取消");
        customizeButton(cancelButton, RED_BUTTON_COLOR);
        cancelButton.addActionListener(e -> handleCancel());

        // 完成按钮（初始隐藏）
        completeButton = new JButton("完成");
        customizeButton(completeButton, GREEN_BUTTON_COLOR);
        completeButton.addActionListener(e -> dispose()); // 关闭对话框
        completeButton.setVisible(false);

        buttonPanel.add(cancelButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0))); // 按钮间距
        buttonPanel.add(completeButton);
        bottomPanel.add(buttonPanel); // 按钮添加到底部容器


        // 3. 组装主面板：底部容器整体添加到SOUTH方位
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        setContentPane(mainPanel);
    }

    // 按钮样式定制方法
    private void customizeButton(JButton button, Color bgColor) {
        button.setBackground(bgColor);
        button.setForeground(BUTTON_TEXT_COLOR);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setPreferredSize(new Dimension(100, 30));
        // 鼠标悬停效果
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
        // 确保UI更新在EDT线程（Swing线程安全）
        SwingUtilities.invokeLater(() -> {
            tableModel.setValueAt(status, index, 1);

            // 自动滚动到当前更新的行
            scrollToRow(index);

            if (status.startsWith("完成") || status.startsWith("失败") || status.startsWith("跳过")) {
                completedFiles++;
                overallProgress.setValue(completedFiles);
                // 同步进度条文本（如 "3/5 (60%)"）
                overallProgress.setString(completedFiles + "/" + totalFiles + " (" +
                        (totalFiles == 0 ? 0 : (completedFiles * 100 / totalFiles)) + "%)");
            }

        });
    }

    // 滚动到指定行
    private void scrollToRow(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= tableModel.getRowCount()) {
            return; // 无效行索引，直接返回
        }

        // 确保行可见
        progressTable.scrollRectToVisible(progressTable.getCellRect(rowIndex, 0, true));

        // 可选：高亮显示当前行
        progressTable.setRowSelectionInterval(rowIndex, rowIndex);
    }

    // 处理取消操作
    private void handleCancel() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "确定要取消吗？未完成的文件将停止上传/下载。",
                "确认取消",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            isCancelled.set(true);
            cancelButton.setEnabled(false);
            cancelButton.setText("取消中...");
            cancelButton.setBackground(RED_BUTTON_COLOR.brighter());
        }
    }

    // 处理窗口关闭操作（不变）
    private void handleWindowClose() {
        if (isCompleted) {
            dispose();
        } else {
            handleCancel();
        }
    }

    // 任务结束时更新按钮状态（不变）
    public void onTaskFinished() {
        SwingUtilities.invokeLater(() -> {
            cancelButton.setVisible(false);
            completeButton.setVisible(true);
            isCompleted = true;
            setTitle(isCancelled.get() ? getTitle() + " - 已取消" : getTitle() + " - 完成");
        });
    }
}