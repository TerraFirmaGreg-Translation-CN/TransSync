package io.github.tfgcn.transsync;

import com.formdev.flatlaf.FlatLightLaf;
import io.github.tfgcn.transsync.view.MainFrame;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.io.IOException;

@Slf4j
public class App {
    public static void main(String[] args) throws IOException {
        // 设置现代外观
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (UnsupportedLookAndFeelException ignore) {
        }
        
        // 加载配置
        Config config = Config.getInstance();
        
        // 创建并显示GUI
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame(config);
            mainFrame.setVisible(true);
            log.info("Fine");
        });
    }
}