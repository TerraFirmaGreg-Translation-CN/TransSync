package io.github.tfgcn.transsync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.tfgcn.transsync.paratranz.ParatranzApiFactory;
import io.github.tfgcn.transsync.paratranz.api.UsersApi;
import io.github.tfgcn.transsync.paratranz.model.users.UsersDto;
import io.github.tfgcn.transsync.service.model.FileScanRule;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import retrofit2.Response;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static io.github.tfgcn.transsync.Constants.*;

/**
 * Configurations
 *
 * @author yanmaoyuan
 */
@Slf4j
@Getter
@Setter
public final class Config {

    private static Config instance;

    /**
     * paratranz token, get it from <a href="https://paratranz.cn/users/my">paratranz.cn</a>
     */
    private String token;
    /**
     * paratranz project id
     */
    private Integer projectId;
    /**
     * @see io.github.tfgcn.transsync.paratranz.interceptor.LoggingInterceptor.Level
     */
    private String httpLogLevel;
    /**
     * The directory where the source files are located, and where the translated files are stored.
     */
    private String workspace;
    /**
     * The rules for scanning files.
     */
    private List<FileScanRule> rules;

    private Config() {
        // for initialization only
    }

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

        Config config;
        if (!file.exists()) {
            if (!file.createNewFile()) {
                log.error("failed to create file: {}", CONFIG_FILE);
                throw new IOException(MSG_FAILED_CREATE_FILE);
            }

            // Create config file from environment variables, or else use defaults.
            config = new Config();
            config.setToken(getEnv(ENV_PARATRANZ_TOKEN).orElse(MSG_GO_GET_TOKEN));
            config.setProjectId(getEnvAsInt(ENV_PARATRANZ_PROJECT_ID).orElse(DEFAULT_PROJECT_ID));
            config.setHttpLogLevel(getEnv(ENV_HTTP_LOG_LEVEL).orElse(DEFAULT_HTTP_LOG_LEVEL));
            config.setWorkspace(getEnv(ENV_PARATRANZ_WORKSPACE).orElse(DEFAULT_WORKSPACE));

            config.save();
        } else {
            // read from file
            Gson gson = new GsonBuilder().create();
            config = gson.fromJson(new FileReader(file, StandardCharsets.UTF_8), Config.class);

            // override with environment variables
            getEnv(ENV_PARATRANZ_TOKEN).ifPresent(config::setToken);
            getEnvAsInt(ENV_PARATRANZ_PROJECT_ID).ifPresent(config::setProjectId);
            getEnv(ENV_HTTP_LOG_LEVEL).ifPresent(config::setHttpLogLevel);
            getEnv(ENV_PARATRANZ_WORKSPACE).ifPresent(config::setWorkspace);
        }
        return config;
    }

    /**
     * Merge new config into current config
     * @param newConfig new config
     */
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
        if (newConfig.getRules() != null) {
            this.rules = newConfig.getRules();
        }
    }

    public void save() throws IOException {
        File file = new File(CONFIG_FILE);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            gson.toJson(this, writer);
        }
    }

    /**
     * Check paratranz connection by calling UsersApi#my()
     */
    public boolean checkParatranzConnected() throws IOException {
        ParatranzApiFactory factory = new ParatranzApiFactory(this);
        UsersApi usersApi = factory.create(UsersApi.class);
        try {
            Response<UsersDto> response = usersApi.my().execute();
            if (response.isSuccessful()) {
                return true;
            }
        } catch (IOException e) {
            log.error("Paratranz connection failed", e);
            throw e;
        }
        return false;
    }

    /**
     * Get environment variable
     */
    private static Optional<String> getEnv(String key) {
        return Optional.ofNullable(System.getenv(key))
                .filter(value -> !value.trim().isEmpty());
    }

    /**
     * Get environment variable as int
     */
    private static Optional<Integer> getEnvAsInt(String key) {
        return getEnv(key).flatMap(value -> {
            try {
                return Optional.of(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                log.warn("Value {} of {} is not a valid integer", key, value);
                return Optional.empty();
            }
        });
    }
}
