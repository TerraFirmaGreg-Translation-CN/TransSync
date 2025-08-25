package io.github.tfgcn.transsync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.tfgcn.transsync.paratranz.ParatranzApiFactory;
import io.github.tfgcn.transsync.paratranz.api.UsersApi;
import io.github.tfgcn.transsync.paratranz.interceptor.LoggingInterceptor;
import io.github.tfgcn.transsync.paratranz.model.users.UsersDto;
import io.github.tfgcn.transsync.service.model.FileScanRule;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Response;

import java.io.File;
import java.io.IOException;
import java.util.List;

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

    private static Config instance;

    private String token;
    private Integer projectId;
    private String httpLogLevel;
    private String workspace;
    private List<FileScanRule> rules;

    // 私有构造函数，防止外部实例化
    private Config() {
    }

    /**
     * 获取单例实例
     * @return Config单例对象
     * @throws IOException 加载配置文件时可能发生的异常
     */
    public static synchronized Config getInstance() throws IOException {
        if (instance == null) {
            instance = loadConfig();
        }
        return instance;
    }

    /**
     * 实际加载配置的方法
     */
    private static Config loadConfig() throws IOException {
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
            config.setWorkspace(DEFAULT_WORKSPACE);

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
        if (newConfig.getWorkspace() != null) {
            this.workspace = newConfig.getWorkspace();
        }
    }

    public void save() throws IOException {
        File file = new File(CONFIG_FILE);
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter();
        mapper.writeValue(file, this);
    }

    /**
     * 检查Paratranz是否连接成功
     * @return true表示连接成功，false表示连接失败
     * @throws IOException
     */
    public boolean checkParatranzConnected() throws IOException {
        ParatranzApiFactory factory = new ParatranzApiFactory(this);
        UsersApi usersApi = factory.create(UsersApi.class);
        try {
            Response<UsersDto> response = usersApi.my().execute();
            if (response.isSuccessful()) {
                log.info("Paratranz 登录成功: {}", response.body());
                return true;
            }
        } catch (IOException e) {
            log.error("Paratranz 登录失败", e);
            throw e;
        }
        return false;
    }
}
