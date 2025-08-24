package io.github.tfgcn.transsync;

import com.formdev.flatlaf.FlatLightLaf;
import io.github.tfgcn.transsync.cmd.DownloadTranslationsCommand;
import io.github.tfgcn.transsync.cmd.UploadOriginCommand;
import io.github.tfgcn.transsync.cmd.UploadTranslationsCommand;
import io.github.tfgcn.transsync.paratranz.error.ApiException;
import io.github.tfgcn.transsync.gui.MainFrame;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Slf4j
@Command(name = "transsync", mixinStandardHelpOptions = true, version = Constants.VERSION, description = Constants.DESCRIPTION)
public class Main {

    public static void main(String[] args) throws IOException, ApiException {
        if (GraphicsEnvironment.isHeadless()) {
            cmd(args);// 命令行模式
        } else {
            gui();
        }
    }

    private static void gui() throws IOException {
        Config config = Config.getInstance();

        // 设置现代外观
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (UnsupportedLookAndFeelException ignore) {
        }

        // 创建并显示GUI
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame(config);
            mainFrame.setVisible(true);
            log.info("Fine");
        });
    }

    private static void cmd(String[] args) throws IOException {
        CommandLine commandLine = new CommandLine(new Main())
                .addSubcommand(new UploadOriginCommand())
                .addSubcommand(new UploadTranslationsCommand())
                .addSubcommand(new DownloadTranslationsCommand());
        commandLine.setExecutionStrategy(new CommandLine.RunLast());
        commandLine.execute(args);
    }
}
