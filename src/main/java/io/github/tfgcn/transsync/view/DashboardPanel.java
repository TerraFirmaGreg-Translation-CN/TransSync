package io.github.tfgcn.transsync.view;

import io.github.tfgcn.transsync.Config;

import javax.swing.*;
import java.awt.*;
import java.util.Date;

public class DashboardPanel extends JPanel {
    private final Config config;

    private JLabel paratranzStatusLabel;
    private JLabel lastSyncLabel;
    private JButton syncNowButton;
    private JProgressBar progressBar;
    
    public DashboardPanel(Config config) {
        this.config = config;

        initComponents();
        setupLayout();
        setupEventHandlers();
        updateStatus();
    }
    
    private void initComponents() {
        paratranzStatusLabel = new JLabel("检查中...");
        lastSyncLabel = new JLabel("从未同步");
        
        syncNowButton = new JButton("立即同步");
        progressBar = new JProgressBar();
        progressBar.setVisible(false);
    }
    
    private void setupLayout() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("Paratranz 状态:"), gbc);
        
        gbc.gridx = 1;
        add(paratranzStatusLabel, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("最后同步时间:"), gbc);
        
        gbc.gridx = 1;
        add(lastSyncLabel, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        add(syncNowButton, gbc);
        
        gbc.gridy = 3;
        add(progressBar, gbc);
    }
    
    private void setupEventHandlers() {
        syncNowButton.addActionListener(e -> startSync());
    }
    
    private void updateStatus() {
        // 检查 Paratranz 连接状态
        new Thread(() -> {
            try {
                boolean paratranzConnected = false;
                // TODO检测paratranz项目
                SwingUtilities.invokeLater(() -> 
                    paratranzStatusLabel.setText(paratranzConnected ? "已连接" : "连接失败"));
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> 
                    paratranzStatusLabel.setText("连接错误"));
            }
        }).start();
    }
    
    private void startSync() {
        syncNowButton.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        
        new Thread(() -> {
            try {
                // TODO
                SwingUtilities.invokeLater(() -> {
                    lastSyncLabel.setText(new Date().toString());
                    progressBar.setVisible(false);
                    syncNowButton.setEnabled(true);
                    JOptionPane.showMessageDialog(this, "同步完成", "成功", JOptionPane.INFORMATION_MESSAGE);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setVisible(false);
                    syncNowButton.setEnabled(true);
                    JOptionPane.showMessageDialog(this, "同步失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }
    
    public void updateConfig(Config config) {
        this.config.merge(config);
        updateStatus();
    }
}