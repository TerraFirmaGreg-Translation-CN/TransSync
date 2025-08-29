package io.github.tfgcn.transsync.gui;

import javax.swing.*;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 自定义 OutputStream，将输出重定向到 LogPanel
 */
class LogPanelOutputStream extends OutputStream {
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