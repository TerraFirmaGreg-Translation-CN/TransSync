package io.github.tfgcn.transsync;

import com.formdev.flatlaf.FlatLightLaf;
import com.google.common.base.Preconditions;
import io.github.tfgcn.transsync.cmd.UploadOriginCommand;
import io.github.tfgcn.transsync.service.SyncService;
import io.github.tfgcn.transsync.paratranz.error.ApiException;
import io.github.tfgcn.transsync.paratranz.ParatranzApiFactory;
import io.github.tfgcn.transsync.paratranz.api.FilesApi;
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
                .addSubcommand(new UploadOriginCommand());
        commandLine.setExecutionStrategy(new CommandLine.RunLast());
        commandLine.execute(args);
    }

    private static void uploadOriginals(String[] args, Config config) throws IOException {

        Integer projectId = config.getProjectId();
        Preconditions.checkArgument(projectId != null && projectId > 0,
                "项目ID不能为空，请前往 https://paratranz.cn 获取项目ID，并写入配置文件");

        ParatranzApiFactory paratranzApiFactory = new ParatranzApiFactory(config);
        FilesApi filesApi = paratranzApiFactory.create(FilesApi.class);

        SyncService app = new SyncService();
        app.setFilesApi(filesApi);
        app.setProjectId(projectId);

        // 默认本项目与 Tools-Modern 处于同级目录，因此直接到上级目录查找即可
        app.setWorkspace("..");

        // 扫描所有需要汉化的文件
        app.uploadOriginals();
    }
}
