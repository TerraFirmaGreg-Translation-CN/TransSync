package io.github.tfgcn.transsync.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Vector;

public class UploadProgressDialog extends JDialog {
    private JTable progressTable;
    private DefaultTableModel tableModel;
    private JProgressBar overallProgress;
    private JLabel progressLabel;
    private int totalFiles;
    private int completedFiles;

    public UploadProgressDialog(Frame owner, String title, List<FileItem> fileItems) {
        super(owner, title, true);
        this.totalFiles = fileItems.size();
        this.completedFiles = 0;

        // 初始化UI
        initUI(fileItems);

        // 设置对话框属性
        setSize(700, 500);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE); // 禁止关闭
    }

    private void initUI(List<FileItem> fileItems) {
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
        for (FileItem item : fileItems) {
            tableModel.addRow(new Vector() {{
                add(item.getFilename());
                add("等待");
            }});
        }

        // 创建表格并设置滚动面板（固定高度）
        progressTable = new JTable(tableModel);
        progressTable.getColumnModel().getColumn(0).setPreferredWidth(450);
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

        // 组装面板
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)), BorderLayout.SOUTH);
        mainPanel.add(progressPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
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
}