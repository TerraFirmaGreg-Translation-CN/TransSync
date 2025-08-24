package io.github.tfgcn.transsync.cmd;

import io.github.tfgcn.transsync.Config;
import io.github.tfgcn.transsync.Constants;
import io.github.tfgcn.transsync.paratranz.interceptor.LoggingInterceptor;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.IOException;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
@Slf4j
public abstract class BaseCommand {

    @CommandLine.Option(names = {"-t", "--token"}, description = "Paratranz Token。前往 https://paratranz.cn/users/my 点击设置获取，并保存到 config.json 配置文件中")
    protected String token;

    @CommandLine.Option(names = {"-p", "--project-id"}, description = "Paratranz 项目ID.")
    protected Integer projectId;

    @CommandLine.Option(names = {"-w", "--workspace"}, description = "工作目录，Tools-Modern 项目应位于此目录下。", defaultValue = Constants.DEFAULT_WORKSPACE)
    protected String workspace;

    @CommandLine.Option(names = {"-l", "--http-level"}, description = "HTTP日志等级. [NONE, BASIC, HEADERS, BODY]")
    protected LoggingInterceptor.Level httpLogLevel;

    protected Config initConfig() throws IOException {
        Config config = Config.getInstance();

        if (token != null) {
            config.setToken(token);
            boolean connected = config.checkParatranzConnected();
            if (!connected) {
                log.error(Constants.MSG_TOKEN_NOT_EXISTS);
                return null;
            }
        }

        if (projectId != null) {
            config.setProjectId(projectId);
        }

        if (workspace != null) {
            config.setWorkspace(workspace);
        }

        if (httpLogLevel != null) {
            config.setHttpLogLevel(httpLogLevel.name());
        }

        return config;
    }
}
