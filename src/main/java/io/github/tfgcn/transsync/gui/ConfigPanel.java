package io.github.tfgcn.transsync.gui;

import io.github.tfgcn.transsync.Config;
import io.github.tfgcn.transsync.Constants;
import io.github.tfgcn.transsync.service.model.FileScanRule;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ConfigPanel extends JPanel {
    private final transient Config config;
    private final transient List<FileScanRule> rules = new ArrayList<>();

    private JTextField paratranzTokenField;
    private JTextField paratranzProjectIdField;
    private JTextField workspaceField;
    private JButton browseButton;
    private JButton saveButton;
    private JButton addRuleButton;
    private JTable rulesTable;
    private DefaultTableModel rulesTableModel;
    
    private final List<ConfigChangeListener> listeners = new ArrayList<>();
    
    public ConfigPanel(Config config) {
        this.config = config;

        initComponents();
        setupLayout();
        setupEventHandlers();
        loadConfig();
    }
    
    private void initComponents() {
        paratranzTokenField = new JTextField(30);
        paratranzProjectIdField = new JTextField(10);
        workspaceField = new JTextField(30);
        browseButton = new JButton("浏览...");
        saveButton = new JButton("保存配置");
        
        // 初始化规则表格相关组件
        addRuleButton = new JButton("添加规则");
        String[] columnNames = {"源文件模式", "翻译文件模式", "原文语言", "译文语言", "操作"};
        rulesTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 4; // 只有最后一列(操作列)可编辑
            }
        };
        rulesTable = new JTable(rulesTableModel);
        rulesTable.getColumnModel().getColumn(4).setCellRenderer(new ButtonRenderer());
        rulesTable.getColumnModel().getColumn(4).setCellEditor(new ButtonEditor(new JCheckBox()));
        // 固定列度，后3列比较窄，前3列自动缩放
        for (int i = 2; i < rulesTableModel.getColumnCount(); i++) {
            rulesTable.getColumnModel().getColumn(i).setPreferredWidth(80);
            rulesTable.getColumnModel().getColumn(i).setMaxWidth(80);
            rulesTable.getColumnModel().getColumn(i).setMinWidth(80);
        }
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
        gbc.gridx = 1;
        gbc.gridy = row++;
        gbc.weightx = 1.0; // 允许水平扩展
        add(paratranzTokenField, gbc);

        add(new JLabel("项目ID:"), createLabelConstraints(0, row));
        gbc.gridx = 1;
        gbc.gridy = row++;
        // 保持weightx=1.0，继续允许水平扩展
        add(paratranzProjectIdField, gbc);

        // 本地配置
        add(new JLabel("本地配置"), createLabelConstraints(0, row++, 2));

        add(new JLabel("工作目录:"), createLabelConstraints(0, row));
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.fill = GridBagConstraints.HORIZONTAL; // 确保水平填充
        add(workspaceField, gbc);

        gbc.gridx = 2;
        gbc.gridy = row++;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        add(browseButton, gbc);

        // 恢复默认设置，为后续组件做准备
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;

        // 映射规则配置
        add(new JLabel("映射规则配置"), createLabelConstraints(0, row++, 3));

        // 添加规则按钮
        gbc.gridx = 2;// 放在最右侧列
        gbc.gridy = row++;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        addRuleButton.setPreferredSize(new Dimension(100, 25)); // 设置按钮大小
        add(addRuleButton, gbc);

        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 规则表格
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        add(new JScrollPane(rulesTable), gbc);
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 1;

        // 保存按钮
        gbc.gridx = 1;
        gbc.gridy = row + 2;
        gbc.gridwidth = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
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
        addRuleButton.addActionListener(e -> showRuleDialog(null));
    }
    
    private void browseLocalPath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            workspaceField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }
    
    private void loadConfig() {
        paratranzTokenField.setText(config.getToken());
        paratranzProjectIdField.setText(config.getProjectId() != null ? config.getProjectId().toString() :
                Constants.DEFAULT_PROJECT_ID.toString());
        workspaceField.setText(config.getWorkspace());
        
        // 加载规则
        rules.clear();
        if (config.getRules() != null) {
            rules.addAll(config.getRules());
        }
        updateRulesTable();
    }
    
    private void saveConfig() {
        try {
            config.setToken(paratranzTokenField.getText());
            config.setProjectId(Integer.parseInt(paratranzProjectIdField.getText()));
            config.setWorkspace(workspaceField.getText());
            config.setRules(rules); // 保存规则

            notifyConfigChanged(config);
            
            JOptionPane.showMessageDialog(this, "配置保存成功", "成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "保存失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateRulesTable() {
        // 清空表格
        rulesTableModel.setRowCount(0);
        
        // 添加所有规则
        for (FileScanRule rule : rules) {
            Object[] row = {
                rule.getSourcePattern(),
                rule.getTranslationPattern(),
                rule.getSrcLang(),
                rule.getDestLang(),
                "编辑"
            };
            rulesTableModel.addRow(row);
        }
    }
    
    private void showRuleDialog(FileScanRule existingRule) {
        // 创建规则编辑对话框
        RuleDialog dialog = new RuleDialog(SwingUtilities.getWindowAncestor(this), existingRule, config.getWorkspace());
        dialog.setVisible(true);
        
        // 如果用户点击了确认按钮，更新规则列表
        if (dialog.isConfirmed()) {
            FileScanRule rule = dialog.getRule();
            if (existingRule != null) {
                // 编辑现有规则
                int index = rules.indexOf(existingRule);
                rules.set(index, rule);
            } else {
                // 添加新规则
                rules.add(rule);
            }
            updateRulesTable();
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
    
    // 按钮渲染器，用于在表格中显示按钮
    private class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                setBackground(UIManager.getColor("Button.background"));
            }
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }
    
    // 按钮编辑器，用于处理表格中按钮的点击事件
    private class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private String label;
        private boolean isPushed;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            if (isSelected) {
                button.setForeground(table.getSelectionForeground());
                button.setBackground(table.getSelectionBackground());
            } else {
                button.setForeground(table.getForeground());
                button.setBackground(table.getBackground());
            }
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                // 获取当前选中行的规则并显示编辑对话框
                int row = rulesTable.getSelectedRow();
                if (row >= 0 && row < rules.size()) {
                    FileScanRule rule = rules.get(row);
                    showRuleDialog(rule);
                }
            }
            isPushed = false;
            return label;
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }
}
