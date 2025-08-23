package io.github.tfgcn.transsync.view;

import io.github.tfgcn.transsync.Config;
import io.github.tfgcn.transsync.Constants;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ConfigPanel extends JPanel {
    private final Config config;

    private JTextField githubTokenField;
    private JTextField paratranzTokenField;
    private JTextField githubRepoField;
    private JTextField paratranzProjectIdField;
    private JTextField localPathField;
    private JButton browseButton;
    private JCheckBox autoSyncCheckbox;
    private JSpinner syncIntervalSpinner;
    private JButton saveButton;
    
    private List<ConfigChangeListener> listeners = new ArrayList<>();
    
    public ConfigPanel(Config config) {
        this.config = config;

        initComponents();
        setupLayout();
        setupEventHandlers();
        loadConfig();
    }
    
    private void initComponents() {
        githubTokenField = new JTextField(30);
        paratranzTokenField = new JTextField(30);
        githubRepoField = new JTextField(30);
        paratranzProjectIdField = new JTextField(10);
        localPathField = new JTextField(30);
        browseButton = new JButton("浏览...");
        
        autoSyncCheckbox = new JCheckBox("自动同步");
        syncIntervalSpinner = new JSpinner(new SpinnerNumberModel(60, 1, 1440, 1));
        saveButton = new JButton("保存配置");
    }
    
    private void setupLayout() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        int row = 0;

        // Paratranz 配置
        add(new JLabel("Paratranz 配置"), createLabelConstraints(0, row++, 2));
        
        add(new JLabel("Token:"), createLabelConstraints(0, row));
        gbc.gridx = 1; gbc.gridy = row++;
        add(paratranzTokenField, gbc);
        
        add(new JLabel("项目ID:"), createLabelConstraints(0, row));
        gbc.gridx = 1; gbc.gridy = row++;
        add(paratranzProjectIdField, gbc);

        // 本地配置
        add(new JLabel("本地配置"), createLabelConstraints(0, row++, 2));
        
        add(new JLabel("工作目录:"), createLabelConstraints(0, row));
        gbc.gridx = 1; gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        add(localPathField, gbc);
        
        gbc.gridx = 2; gbc.gridy = row++;
        add(browseButton, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 保存按钮
        gbc.gridx = 1; gbc.gridy = row + 2; gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        add(saveButton, gbc);
    }
    
    private GridBagConstraints createLabelConstraints(int x, int y, int width) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x; gbc.gridy = y; gbc.gridwidth = width;
        gbc.insets = new Insets(15, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        return gbc;
    }
    
    private GridBagConstraints createLabelConstraints(int x, int y) {
        return createLabelConstraints(x, y, 1);
    }
    
    private void setupEventHandlers() {
        browseButton.addActionListener(e -> browseLocalPath());
        saveButton.addActionListener(e -> saveConfig());
    }
    
    private void browseLocalPath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            localPathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }
    
    private void loadConfig() {
        githubTokenField.setText("");
        paratranzTokenField.setText(config.getToken());
        githubRepoField.setText("");
        paratranzProjectIdField.setText(config.getProjectId() != null ?
            config.getProjectId().toString() : Constants.DEFAULT_PROJECT_ID.toString());
        localPathField.setText(config.getWorkspace());
        autoSyncCheckbox.setSelected(false);
        syncIntervalSpinner.setValue(0);
    }
    
    private void saveConfig() {
        try {
            config.setToken(paratranzTokenField.getText());
            config.setProjectId(Integer.parseInt(paratranzProjectIdField.getText()));
            config.setWorkspace(localPathField.getText());

            notifyConfigChanged(config);
            
            JOptionPane.showMessageDialog(this, "配置保存成功", "成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "保存失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void addConfigChangeListener(ConfigChangeListener listener) {
        listeners.add(listener);
    }
    
    private void notifyConfigChanged(Config newConfig) {
        for (ConfigChangeListener listener : listeners) {
            listener.onConfigChanged(newConfig);
        }
    }
    
    public interface ConfigChangeListener {
        void onConfigChanged(Config newConfig);
    }
}