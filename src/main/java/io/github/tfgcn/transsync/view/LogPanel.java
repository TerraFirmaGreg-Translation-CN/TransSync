package io.github.tfgcn.transsync.view;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class LogPanel extends JPanel {
    private JTextPane logTextPane;
    private JScrollPane scrollPane;
    private JToolBar toolBar;
    
    // 日志级别过滤
    private JComboBox<String> logLevelComboBox;
    private JTextField searchField;
    private JButton clearButton;
    private JCheckBox autoScrollCheckBox;

    // 日志队列，用于线程安全的日志添加
    private final BlockingQueue<LogEntry> logQueue = new LinkedBlockingQueue<>();
    
    // 保存原始的系统输出流
    private PrintStream originalOut;
    private PrintStream originalErr;
    
    // 自定义输出流
    private LogPanelOutputStream logPanelOut;
    private LogPanelOutputStream logPanelErr;
    
    // 日志级别
    public enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
    
    // 日志条目
    @Data
    @AllArgsConstructor
    private static class LogEntry {
        private Date timestamp;
        private LogLevel level;
        private String message;
        private String source;
    }
    
    // 日志样式
    private Style debugStyle;
    private Style infoStyle;
    private Style warnStyle;
    private Style errorStyle;
    private Style defaultStyle;
    
    // 当前过滤级别
    private LogLevel currentFilterLevel = LogLevel.DEBUG;
    
    public LogPanel() {
        initComponents();
        setupLayout();
        setupEventHandlers();
        startLogConsumer();
        
        // 重定向系统输出
        redirectSystemStreams();
    }
    
    private void initComponents() {
        // 创建文本区域
        logTextPane = new JTextPane();
        logTextPane.setEditable(false);
        logTextPane.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        // 创建滚动面板
        scrollPane = new JScrollPane(logTextPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        // 创建工具栏
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        
        // 日志级别过滤
        toolBar.add(new JLabel("日志级别:"));
        logLevelComboBox = new JComboBox<>(new String[]{"全部", "调试", "信息", "警告", "错误"});
        toolBar.add(logLevelComboBox);
        
        // 搜索框
        toolBar.addSeparator();
        toolBar.add(new JLabel("搜索:"));
        searchField = new JTextField(15);
        toolBar.add(searchField);
        
        // 清空按钮
        toolBar.addSeparator();
        clearButton = new JButton("清空");
        toolBar.add(clearButton);
        
        // 自动滚动
        toolBar.addSeparator();
        autoScrollCheckBox = new JCheckBox("自动滚动", true);
        toolBar.add(autoScrollCheckBox);
        
        // 创建样式
        StyledDocument doc = logTextPane.getStyledDocument();
        
        defaultStyle = doc.addStyle("default", null);
        StyleConstants.setForeground(defaultStyle, Color.BLACK);
        
        debugStyle = doc.addStyle("debug", null);
        StyleConstants.setForeground(debugStyle, Color.GRAY);
        
        infoStyle = doc.addStyle("info", null);
        StyleConstants.setForeground(infoStyle, Color.BLUE);
        
        warnStyle = doc.addStyle("warn", null);
        StyleConstants.setForeground(warnStyle, Color.ORANGE);
        
        errorStyle = doc.addStyle("error", null);
        StyleConstants.setForeground(errorStyle, Color.RED);
        StyleConstants.setBold(errorStyle, true);
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private void setupEventHandlers() {
        // 日志级别过滤
        logLevelComboBox.addActionListener(e -> {
            String selected = (String) logLevelComboBox.getSelectedItem();
            switch (selected) {
                case "全部": currentFilterLevel = LogLevel.DEBUG; break;
                case "调试": currentFilterLevel = LogLevel.DEBUG; break;
                case "信息": currentFilterLevel = LogLevel.INFO; break;
                case "警告": currentFilterLevel = LogLevel.WARN; break;
                case "错误": currentFilterLevel = LogLevel.ERROR; break;
            }
            filterLogs();
        });
        
        // 搜索功能
        searchField.addActionListener(e -> searchLogs());
        
        // 清空日志
        clearButton.addActionListener(e -> clearLogs());
    }

    /**
     * 重定向系统输出流到 LogPanel
     */
    private void redirectSystemStreams() {
        // 保存原始的系统输出流
        originalOut = System.out;
        originalErr = System.err;
        
        // 创建自定义输出流
        logPanelOut = new LogPanelOutputStream(this, LogLevel.INFO, false);
        logPanelErr = new LogPanelOutputStream(this, LogLevel.ERROR, true);
        
        // 创建新的 PrintStream
        PrintStream printStreamOut = new PrintStream(logPanelOut, true);
        PrintStream printStreamErr = new PrintStream(logPanelErr, true);
        
        // 重定向系统输出
        System.setOut(printStreamOut);
        System.setErr(printStreamErr);
        
        // 添加关闭钩子，确保在程序退出时恢复原始输出流
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }));
    }
    
    // 其他方法（启动日志消费者、添加日志、过滤日志、搜索日志、清空日志、导出日志等）...
    // 这些方法与之前的实现相同，不再重复
    
    // 启动日志消费者线程
    private void startLogConsumer() {
        Thread consumerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    LogEntry entry = logQueue.take();
                    addLogToUI(entry);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        consumerThread.setDaemon(true);
        consumerThread.setName("LogConsumer");
        consumerThread.start();
    }
    
    // 添加日志到UI（线程安全）
    private void addLogToUI(LogEntry entry) {
        SwingUtilities.invokeLater(() -> {
            // 检查日志级别过滤
            if (entry.getLevel().ordinal() < currentFilterLevel.ordinal()) {
                return;
            }
            
            StyledDocument doc = logTextPane.getStyledDocument();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            try {
                // 添加时间戳
                doc.insertString(doc.getLength(), "[" + sdf.format(entry.getTimestamp()) + "] ", defaultStyle);
                
                // 添加日志级别
                String levelStr;
                Style style;
                switch (entry.getLevel()) {
                    case DEBUG:
                        levelStr = "DEBUG";
                        style = debugStyle;
                        break;
                    case INFO:
                        levelStr = "INFO";
                        style = infoStyle;
                        break;
                    case WARN:
                        levelStr = "WARN";
                        style = warnStyle;
                        break;
                    case ERROR:
                        levelStr = "ERROR";
                        style = errorStyle;
                        break;
                    default:
                        levelStr = "UNKNOWN";
                        style = defaultStyle;
                }
                doc.insertString(doc.getLength(), "[" + levelStr + "] ", style);
                
                // 添加来源（如果有）
                if (entry.getSource() != null && !entry.getSource().isEmpty()) {
                    doc.insertString(doc.getLength(), "[" + entry.getSource() + "] ", defaultStyle);
                }
                
                // 添加消息
                doc.insertString(doc.getLength(), entry.getMessage() + "\n", defaultStyle);
                
                // 自动滚动到底部
                if (autoScrollCheckBox.isSelected()) {
                    logTextPane.setCaretPosition(doc.getLength());
                }
                
                // 限制日志行数，避免内存占用过大
                limitLogLines(1000);
            } catch (BadLocationException e) {
                log.error("添加日志到UI失败", e);
            }
        });
    }
    
    // 限制日志行数
    private void limitLogLines(int maxLines) {
        StyledDocument doc = logTextPane.getStyledDocument();
        try {
            int lineCount = doc.getDefaultRootElement().getElementCount();
            if (lineCount > maxLines) {
                int removeCount = lineCount - maxLines / 2; // 保留一半
                int endOffset = doc.getDefaultRootElement().getElement(removeCount).getEndOffset();
                doc.remove(0, endOffset);
            }
        } catch (BadLocationException e) {
            log.error("限制日志行数失败", e);
        }
    }
    
    // 添加日志条目
    public void addLog(LogLevel level, String message) {
        addLog(level, message, null);
    }
    
    public void addLog(LogLevel level, String message, String source) {
        LogEntry entry = new LogEntry(new Date(), level, message, source);
        try {
            logQueue.put(entry);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("添加日志到队列失败", e);
        }
    }
    
    // 添加调试日志
    public void debug(String message) {
        addLog(LogLevel.DEBUG, message);
    }
    
    public void debug(String message, String source) {
        addLog(LogLevel.DEBUG, message, source);
    }
    
    // 添加信息日志
    public void info(String message) {
        addLog(LogLevel.INFO, message);
    }
    
    public void info(String message, String source) {
        addLog(LogLevel.INFO, message, source);
    }
    
    // 添加警告日志
    public void warn(String message) {
        addLog(LogLevel.WARN, message);
    }
    
    public void warn(String message, String source) {
        addLog(LogLevel.WARN, message, source);
    }
    
    // 添加错误日志
    public void error(String message) {
        addLog(LogLevel.ERROR, message);
    }
    
    public void error(String message, String source) {
        addLog(LogLevel.ERROR, message, source);
    }
    
    public void error(String message, Throwable throwable) {
        addLog(LogLevel.ERROR, message + ": " + throwable.getMessage());
    }
    
    public void error(String message, Throwable throwable, String source) {
        addLog(LogLevel.ERROR, message + ": " + throwable.getMessage(), source);
    }
    
    // 过滤日志
    private void filterLogs() {
        // 保存当前滚动位置
        int caretPosition = logTextPane.getCaretPosition();
        
        // 清空当前显示
        logTextPane.setText("");
        
        // 重新添加所有符合过滤条件的日志
        for (LogEntry entry : logQueue) {
            if (entry.getLevel().ordinal() >= currentFilterLevel.ordinal()) {
                addLogToUI(entry);
            }
        }
        
        // 恢复滚动位置
        logTextPane.setCaretPosition(Math.min(caretPosition, logTextPane.getDocument().getLength()));
    }
    
    // 搜索日志
    private void searchLogs() {
        String searchText = searchField.getText().trim();
        if (searchText.isEmpty()) {
            return;
        }
        
        String content = logTextPane.getText();
        int index = content.indexOf(searchText, logTextPane.getCaretPosition() + 1);
        
        if (index == -1) {
            // 从开头重新搜索
            index = content.indexOf(searchText);
        }
        
        if (index != -1) {
            logTextPane.setCaretPosition(index);
            logTextPane.moveCaretPosition(index + searchText.length());
            logTextPane.getCaret().setSelectionVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, 
                "未找到: " + searchText, 
                "搜索", 
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    // 清空日志
    private void clearLogs() {
        int result = JOptionPane.showConfirmDialog(this,
            "确定要清空所有日志吗？",
            "清空日志",
            JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            logTextPane.setText("");
            logQueue.clear();
        }
    }

    // 设置是否自动滚动
    public void setAutoScroll(boolean autoScroll) {
        autoScrollCheckBox.setSelected(autoScroll);
    }
    
    // 获取是否自动滚动
    public boolean isAutoScroll() {
        return autoScrollCheckBox.isSelected();
    }
}

/**
 * 自定义 OutputStream，将输出重定向到 LogPanel
 */
class LogPanelOutputStream extends OutputStream {
    private final LogPanel logPanel;
    private final LogPanel.LogLevel level;
    private final StringBuilder buffer;
    private final boolean isStdErr;
    
    public LogPanelOutputStream(LogPanel logPanel, LogPanel.LogLevel level, boolean isStdErr) {
        this.logPanel = logPanel;
        this.level = level;
        this.buffer = new StringBuilder();
        this.isStdErr = isStdErr;
    }
    
    @Override
    public void write(int b) throws IOException {
        synchronized (buffer) {
            // 将字节转换为字符
            char c = (char) b;
            
            // 如果是换行符，则刷新缓冲区
            if (c == '\n') {
                flush();
                return;
            }
            
            // 将字符添加到缓冲区
            buffer.append(c);
        }
    }
    
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        synchronized (buffer) {
            // 将字节数组转换为字符串
            String s = new String(b, off, len, StandardCharsets.UTF_8);
            
            // 处理字符串中的换行符
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '\n') {
                    flush();
                } else {
                    buffer.append(c);
                }
            }
        }
    }
    
    @Override
    public void flush() throws IOException {
        synchronized (buffer) {
            // 如果缓冲区有内容，则发送到 LogPanel
            if (!buffer.isEmpty()) {
                String message = buffer.toString();
                
                // 清理 ANSI 转义序列
                String msg = cleanAnsiCodes(message);
                
                // 在 EDT 线程中更新 UI
                SwingUtilities.invokeLater(() -> {
                    if (isStdErr) {
                        logPanel.error(msg, "System.err");
                    } else {
                        logPanel.info(msg, "System.out");
                    }
                });
                
                // 清空缓冲区
                buffer.setLength(0);
            }
        }
    }
    
    @Override
    public void close() throws IOException {
        flush();
    }
    
    /**
     * 清理 ANSI 转义序列
     */
    private String cleanAnsiCodes(String text) {
        // 移除 ANSI 转义序列
        return text.replaceAll("\u001B\\[[;\\d]*m", "");
    }
}