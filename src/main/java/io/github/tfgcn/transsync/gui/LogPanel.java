package io.github.tfgcn.transsync.gui;

import io.github.tfgcn.transsync.I18n;
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
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS;

@Slf4j
public class LogPanel extends JPanel {
    private JTextPane logTextPane;
    private JScrollPane scrollPane;
    private JToolBar toolBar;
    
    private JButton clearButton;
    private JCheckBox autoScrollCheckBox;

    // 日志队列，用于线程安全的日志添加
    private final BlockingQueue<LogEntry> logQueue = new LinkedBlockingQueue<>();

    // 日志级别
    public enum LogLevel {
        INFO, ERROR
    }
    
    // 日志条目
    @Data
    @AllArgsConstructor
    private static class LogEntry {
        private Date timestamp;
        private LogLevel level;
        private String message;
    }

    // 日志样式
    private transient Style errorStyle;
    private transient Style defaultStyle;
    
    public LogPanel() {
        initComponents();
        setLocalizedText();
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
        scrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
        
        // 创建工具栏
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        
        // 清空按钮
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
        // 清空日志
        clearButton.addActionListener(e -> clearLogs());
    }

    /**
     * 重定向系统输出流到 LogPanel
     */
    private void redirectSystemStreams() {
        // 保存原始的系统输出流
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        
        // 创建自定义输出流
        LogPanelOutputStream logPanelOut = new LogPanelOutputStream(this, false);
        LogPanelOutputStream logPanelErr = new LogPanelOutputStream(this, true);

        // 重定向系统输出
        System.setOut(new PrintStream(logPanelOut, true));
        System.setErr(new PrintStream(logPanelErr, true));

        // 添加关闭钩子，确保在程序退出时恢复原始输出流
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }));
    }
    
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

            StyledDocument doc = logTextPane.getStyledDocument();
            try {
                // 添加日志级别
                Style style;
                if (entry.getLevel() == LogLevel.ERROR) {
                    style = errorStyle;
                } else {
                    style = defaultStyle;
                }

                // 添加消息
                doc.insertString(doc.getLength(), entry.getMessage() + "\n", style);
                
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
        LogEntry entry = new LogEntry(new Date(), level, message);
        try {
            logQueue.put(entry);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("添加日志到队列失败", e);
        }
    }
    
    // 添加信息日志
    public void info(String message) {
        addLog(LogLevel.INFO, message);
    }

    // 添加错误日志
    public void error(String message) {
        addLog(LogLevel.ERROR, message);
    }

    // 清空日志
    private void clearLogs() {
        int result = JOptionPane.showConfirmDialog(this,
                I18n.getString("dialog.clearLog.message"),// "确定要清空所有日志吗？"
            I18n.getString("dialog.clearLog.title"),// "清空日志",
            JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            logTextPane.setText("");
            logQueue.clear();
        }
    }

    void setLocalizedText() {
        clearButton.setText(I18n.getString("panel.log.button.clear"));
        autoScrollCheckBox.setText(I18n.getString("panel.log.checkbox.autoScroll"));
    }

    /**
     * 自定义 OutputStream，将输出重定向到 LogPanel
     */
    static class LogPanelOutputStream extends OutputStream {
        private final LogPanel logPanel;
        private final StringBuilder buffer;
        private final boolean isStdErr;

        public LogPanelOutputStream(LogPanel logPanel, boolean isStdErr) {
            this.logPanel = logPanel;
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
                            logPanel.error(msg);
                        } else {
                            logPanel.info(msg);
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
}
