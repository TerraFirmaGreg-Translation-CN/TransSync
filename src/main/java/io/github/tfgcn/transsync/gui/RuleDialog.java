package io.github.tfgcn.transsync.gui;

import io.github.tfgcn.transsync.service.model.FileScanRequest;
import io.github.tfgcn.transsync.service.model.FileScanResult;
import io.github.tfgcn.transsync.service.model.FileScanRule;
import io.github.tfgcn.transsync.service.FileScanService;
import lombok.Getter;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;

public class RuleDialog extends JDialog {
    @Getter
    private boolean confirmed = false;
    @Getter
    private final transient FileScanRule rule;

    // UI 组件
    private JTextField sourcePatternField;
    private JTextField translationPatternField;
    private JTextField srcLangField;
    private JTextField languageField;
    private JTextArea ignoresTextArea; // 忽略规则输入框
    private JTree sourceTree;
    private JTree translationTree;
    private DefaultTreeModel sourceTreeModel;
    private DefaultTreeModel translationTreeModel;

    private final String workspace;

    public RuleDialog(Window owner, FileScanRule existingRule, String workspace) {
        super(owner, existingRule == null ? "添加映射规则" : "编辑映射规则", ModalityType.APPLICATION_MODAL);
        this.rule = existingRule != null ? new FileScanRule(existingRule) : new FileScanRule();
        this.workspace = workspace;

        initUI();
        loadRuleData();
        setSize(800, 600);
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());

        // 创建顶部配置面板
        JPanel configPanel = createConfigPanel();
        mainPanel.add(configPanel, BorderLayout.NORTH);

        // 创建中部树面板
        JPanel treePanel = createTreePanel();
        mainPanel.add(treePanel, BorderLayout.CENTER);

        // 创建底部按钮面板
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // 添加主面板到窗口
        add(mainPanel);
    }

    private JPanel createConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // 组件间距
        gbc.anchor = GridBagConstraints.WEST; // 左对齐
        gbc.fill = GridBagConstraints.HORIZONTAL; // 水平填充

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.2;
        panel.add(new JLabel("原文匹配模式:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.8;
        sourcePatternField = new JTextField();
        panel.add(sourcePatternField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.2;
        panel.add(new JLabel("译文匹配模式:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.8;
        translationPatternField = new JTextField();
        panel.add(translationPatternField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.2;
        panel.add(new JLabel("原文语言代码:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 0.8;
        srcLangField = new JTextField();
        panel.add(srcLangField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.2;
        panel.add(new JLabel("译文语言代码:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.weightx = 0.8;
        languageField = new JTextField();
        panel.add(languageField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridheight = 2; // 跨 2 行
        gbc.weightx = 0.2;
        gbc.anchor = GridBagConstraints.NORTHWEST; // 顶部左对齐
        panel.add(new JLabel("忽略规则:\n (每行一个模式)"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridheight = 1;
        gbc.weightx = 0.8;
        gbc.weighty = 1.0; // 允许垂直扩展
        ignoresTextArea = new JTextArea(3, 20); // 3 行高度
        ignoresTextArea.setLineWrap(true); // 自动换行
        JScrollPane ignoresScrollPane = new JScrollPane(ignoresTextArea);
        ignoresScrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        panel.add(ignoresScrollPane, gbc);

        JButton scanButton = new JButton("测试扫描规则");
        scanButton.addActionListener(e -> scanFiles());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(scanButton);

        JPanel container = new JPanel(new BorderLayout());
        container.add(panel, BorderLayout.CENTER);
        container.add(buttonPanel, BorderLayout.SOUTH);

        return container;
    }

    private JPanel createTreePanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 创建源文件树
        sourceTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode("原文"));
        sourceTree = new JTree(sourceTreeModel);
        sourceTree.setRootVisible(false);

        // 创建翻译文件树
        translationTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode("译文"));
        translationTree = new JTree(translationTreeModel);
        translationTree.setRootVisible(false);

        // 添加滚动面板
        JScrollPane sourceScroll = new JScrollPane(sourceTree);
        sourceScroll.setBorder(BorderFactory.createTitledBorder("原文"));

        JScrollPane translationScroll = new JScrollPane(translationTree);
        translationScroll.setBorder(BorderFactory.createTitledBorder("译文"));

        panel.add(sourceScroll);
        panel.add(translationScroll);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton okButton = new JButton("确认");
        okButton.addActionListener(e -> onOk());

        JButton cancelButton = new JButton("取消");
        cancelButton.addActionListener(e -> onCancel());

        panel.add(okButton);
        panel.add(cancelButton);

        return panel;
    }

    private void loadRuleData() {
        if (rule != null) {
            sourcePatternField.setText(rule.getSourcePattern());
            translationPatternField.setText(rule.getTranslationPattern());
            srcLangField.setText(rule.getSrcLang());
            languageField.setText(rule.getDestLang());

            // 加载忽略规则
            if (rule.getIgnores() != null && !rule.getIgnores().isEmpty()) {
                StringBuilder ignoresSb = new StringBuilder();
                for (String ignore : rule.getIgnores()) {
                    ignoresSb.append(ignore).append("\n");
                }
                ignoresTextArea.setText(ignoresSb.toString().trim());
            }
        }
    }

    // 扫描文件
    private void scanFiles() {
        if (workspace == null || workspace.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先设置工作目录",
                    "警告", JOptionPane.WARNING_MESSAGE);
            return;
        }

        FileScanRequest request = new FileScanRequest();
        request.setWorkspace(workspace);
        request.setSourceFilePattern(sourcePatternField.getText());
        request.setTranslationFilePattern(translationPatternField.getText());
        request.setSrcLang(srcLangField.getText());
        request.setDestLang(languageField.getText());

        // 处理忽略规则（按行分割）
        List<String> ignores = new ArrayList<>();
        String ignoresText = ignoresTextArea.getText().trim();
        if (!ignoresText.isEmpty()) {
            StringTokenizer tokenizer = new StringTokenizer(ignoresText, "\n");
            while (tokenizer.hasMoreTokens()) {
                String ignore = tokenizer.nextToken().trim();
                if (!ignore.isEmpty()) {
                    ignores.add(ignore);
                }
            }
        }
        request.setIgnores(ignores);

        FileScanService scanService = new FileScanService();
        try {
            List<FileScanResult> results = scanService.scanAndMapFiles(request);
            // 按照源文件路径进行排序
            results.sort(Comparator.comparing(FileScanResult::getSourceFilePath));
            updateTrees(results);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "扫描失败:" + ex.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 更新树显示
    private void updateTrees(List<FileScanResult> results) {
        // 清空树和映射
        sourceTreeModel.setRoot(new DefaultMutableTreeNode("原文"));
        translationTreeModel.setRoot(new DefaultMutableTreeNode("译文"));

        // 添加源文件树节点
        DefaultMutableTreeNode sourceRoot = (DefaultMutableTreeNode) sourceTreeModel.getRoot();
        for (FileScanResult result : results) {
            addToTree(sourceRoot, result.getSourceFilePath());
        }

        // 添加翻译文件树节点
        DefaultMutableTreeNode translationRoot = (DefaultMutableTreeNode) translationTreeModel.getRoot();
        for (FileScanResult result : results) {
            addToTree(translationRoot, result.getTranslationFilePath());
        }

        // 刷新树
        sourceTreeModel.reload();
        translationTreeModel.reload();

        // 展开所有节点
        expandAll(sourceTree, new TreePath(sourceRoot));
        expandAll(translationTree, new TreePath(translationRoot));
    }

    // 添加路径到树
    private void addToTree(DefaultMutableTreeNode root, String filePath) {
        String[] parts = filePath.split("/");
        DefaultMutableTreeNode currentNode = root;
        for (String part : parts) {
            DefaultMutableTreeNode child = findChild(currentNode, part);
            if (child == null) {
                child = new DefaultMutableTreeNode(part);
                currentNode.add(child);
            }

            currentNode = child;
        }
    }

    // 查找子节点
    private DefaultMutableTreeNode findChild(DefaultMutableTreeNode parent, String name) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
            if (name.equals(child.getUserObject())) {
                return child;
            }
        }
        return null;
    }

    // 展开所有树节点
    private void expandAll(JTree tree, TreePath parent) {
        tree.expandPath(parent);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getLastPathComponent();
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            expandAll(tree, parent.pathByAddingChild(child));
        }
    }

    private void onOk() {
        rule.setSourcePattern(sourcePatternField.getText());
        rule.setTranslationPattern(translationPatternField.getText());
        rule.setSrcLang(srcLangField.getText());
        rule.setDestLang(languageField.getText());

        // 保存忽略规则
        List<String> ignores = new ArrayList<>();
        String ignoresText = ignoresTextArea.getText().trim();
        if (!ignoresText.isEmpty()) {
            StringTokenizer tokenizer = new StringTokenizer(ignoresText, "\n");
            while (tokenizer.hasMoreTokens()) {
                String ignore = tokenizer.nextToken().trim();
                if (!ignore.isEmpty()) {
                    ignores.add(ignore);
                }
            }
        }
        rule.setIgnores(ignores);

        confirmed = true;
        dispose();
    }

    private void onCancel() {
        confirmed = false;
        dispose();
    }
}