package io.github.tfgcn.transsync.gui;

import io.github.tfgcn.transsync.service.model.FileScanRequest;
import io.github.tfgcn.transsync.service.model.FileScanResult;
import io.github.tfgcn.transsync.service.model.FileScanRule;
import io.github.tfgcn.transsync.service.FileScanService;
import lombok.Getter;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RuleDialog extends JDialog {
    @Getter
    private boolean confirmed = false;
    @Getter
    private FileScanRule rule;
    
    // UI组件
    private JTextField sourcePatternField;
    private JTextField translationPatternField;
    private JTextField srcLangField;
    private JTextField languageField;
    private JButton scanButton;
    private JTree sourceTree;
    private JTree translationTree;
    private DefaultTreeModel sourceTreeModel;
    private DefaultTreeModel translationTreeModel;
    private JButton okButton;
    private JButton cancelButton;
    
    // 存储树节点映射，用于同步展开/折叠
    private Map<String, DefaultMutableTreeNode> sourceNodeMap = new HashMap<>();
    private Map<String, DefaultMutableTreeNode> translationNodeMap = new HashMap<>();
    
    private String workspace;
    
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
        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 源文件模式输入
        panel.add(new JLabel("源文件模式:"));
        sourcePatternField = new JTextField();
        panel.add(sourcePatternField);

        // 翻译文件模式输入
        panel.add(new JLabel("翻译文件模式:"));
        translationPatternField = new JTextField();
        panel.add(translationPatternField);

        // 源语言代码输入
        panel.add(new JLabel("原文语言代码:"));
        srcLangField = new JTextField();
        panel.add(srcLangField);
        
        // 语言代码输入
        panel.add(new JLabel("译文语言代码:"));
        languageField = new JTextField();
        panel.add(languageField);
        
        // 扫描按钮
        scanButton = new JButton("扫描文件");
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
        sourceTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode("源文件"));
        sourceTree = new JTree(sourceTreeModel);
        translationTree.setRootVisible(false);
        sourceTree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                syncTreeExpansion(event, translationTree, translationNodeMap);
            }
            
            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                syncTreeCollapse(event, translationTree, translationNodeMap);
            }
        });
        
        // 创建翻译文件树
        translationTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode("翻译文件"));
        translationTree = new JTree(translationTreeModel);
        translationTree.setRootVisible(false);
        translationTree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                syncTreeExpansion(event, sourceTree, sourceNodeMap);
            }
            
            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                syncTreeCollapse(event, sourceTree, sourceNodeMap);
            }
        });
        
        // 添加滚动面板
        JScrollPane sourceScroll = new JScrollPane(sourceTree);
        sourceScroll.setBorder(BorderFactory.createTitledBorder("源文件"));
        
        JScrollPane translationScroll = new JScrollPane(translationTree);
        translationScroll.setBorder(BorderFactory.createTitledBorder("翻译文件"));
        
        panel.add(sourceScroll);
        panel.add(translationScroll);
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        okButton = new JButton("确认");
        okButton.addActionListener(e -> onOk());
        
        cancelButton = new JButton("取消");
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
        }
    }
    
    // 同步树展开
    private void syncTreeExpansion(TreeExpansionEvent event, JTree targetTree, 
                                  Map<String, DefaultMutableTreeNode> nodeMap) {
        TreePath path = event.getPath();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        String nodePath = getNodePath(node);
        
        DefaultMutableTreeNode targetNode = nodeMap.get(nodePath);
        if (targetNode != null) {
            TreePath targetPath = new TreePath(targetNode.getPath());
            targetTree.expandPath(targetPath);
        }
    }
    
    // 同步树折叠
    private void syncTreeCollapse(TreeExpansionEvent event, JTree targetTree, 
                                 Map<String, DefaultMutableTreeNode> nodeMap) {
        TreePath path = event.getPath();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        String nodePath = getNodePath(node);
        
        DefaultMutableTreeNode targetNode = nodeMap.get(nodePath);
        if (targetNode != null) {
            TreePath targetPath = new TreePath(targetNode.getPath());
            targetTree.collapsePath(targetPath);
        }
    }
    
    // 获取节点的完整路径
    private String getNodePath(DefaultMutableTreeNode node) {
        StringBuilder path = new StringBuilder();
        Object[] nodes = node.getPath();
        
        for (int i = 0; i < nodes.length; i++) {
            if (i > 0) {
                path.append("/");
            }
            path.append(nodes[i].toString());
        }
        
        return path.toString();
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

        FileScanService scanService = new FileScanService();
        try {
            List<FileScanResult> results = scanService.scanAndMapFiles(request);
            updateTrees(results);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "扫描失败: " + ex.getMessage(), 
                                         "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // 更新树显示
    private void updateTrees(List<FileScanResult> results) {
        // 清空树和映射
        sourceTreeModel.setRoot(new DefaultMutableTreeNode("源文件"));
        translationTreeModel.setRoot(new DefaultMutableTreeNode("翻译文件"));
        sourceNodeMap.clear();
        translationNodeMap.clear();
        
        // 添加源文件树节点
        DefaultMutableTreeNode sourceRoot = (DefaultMutableTreeNode) sourceTreeModel.getRoot();
        for (FileScanResult result : results) {
            addToTree(sourceRoot, result.getSourceFilePath(), sourceNodeMap);
        }
        
        // 添加翻译文件树节点
        DefaultMutableTreeNode translationRoot = (DefaultMutableTreeNode) translationTreeModel.getRoot();
        for (FileScanResult result : results) {
            addToTree(translationRoot, result.getTranslationFilePath(), translationNodeMap);
        }
        
        // 刷新树
        sourceTreeModel.reload();
        translationTreeModel.reload();
        
        // 展开所有节点
        expandAll(sourceTree, new TreePath(sourceRoot));
        expandAll(translationTree, new TreePath(translationRoot));
    }
    
    // 添加路径到树
    private void addToTree(DefaultMutableTreeNode root, String filePath, 
                          Map<String, DefaultMutableTreeNode> nodeMap) {
        String[] parts = filePath.split("/");
        DefaultMutableTreeNode currentNode = root;
        StringBuilder currentPath = new StringBuilder(root.toString());
        
        for (String part : parts) {
            currentPath.append("/").append(part);
            String pathKey = currentPath.toString();
            
            DefaultMutableTreeNode child = findChild(currentNode, part);
            if (child == null) {
                child = new DefaultMutableTreeNode(part);
                currentNode.add(child);
                nodeMap.put(pathKey, child);
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
        
        confirmed = true;
        dispose();
    }
    
    private void onCancel() {
        confirmed = false;
        dispose();
    }
}
