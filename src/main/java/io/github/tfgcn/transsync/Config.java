package io.github.tfgcn.transsync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.tfgcn.transsync.paratranz.interceptor.LoggingInterceptor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;

import static io.github.tfgcn.transsync.Constants.*;

/**
 * 配置文件，单例
 *
 * @author yanmaoyuan
 */
@Slf4j
@Getter
@Setter
public final class Config {

    private String token;
    private Integer projectId;
    private String httpLogLevel;
    private String workspace;

    private Config() {
    }

    public static Config load() throws IOException {
        File file = new File(CONFIG_FILE);

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writerWithDefaultPrettyPrinter();

        Config config;
        if (!file.exists()) {
            // 不存在则创建
            if (!file.createNewFile()) {
                log.error("failed to create file: {}", CONFIG_FILE);
                throw new IOException(MSG_FAILED_CREATE_FILE);
            }

            // 默认配置
            config = new Config();
            config.setToken(MSG_GO_GET_TOKEN);
            config.setProjectId(DEFAULT_PROJECT_ID);
            config.setHttpLogLevel(LoggingInterceptor.Level.NONE.name());

            // 输出到文件
            mapper.writeValue(file, config);
        } else {
            // 读取JSON文件
            config = mapper.readValue(file, Config.class);
        }
        return config;
    }

    public void merge(Config newConfig) {
        if (newConfig.getToken() != null) {
            this.token = newConfig.getToken();
        }
        if (newConfig.getProjectId() != null) {
            this.projectId = newConfig.getProjectId();
        }
        if (newConfig.getHttpLogLevel() != null) {
            this.httpLogLevel = newConfig.getHttpLogLevel();
        }
    }

    public void save() throws IOException {
        File file = new File(CONFIG_FILE);
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter();
        mapper.writeValue(file, this);
    }
}