package io.github.transsync.config;

import io.github.transsync.exception.ConfigLoadException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.LoaderOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 配置管理器：负责加载并验证 config.yml 配置文件
 */
public class ConfigManager {

    private static Config config = null;

    // 配置文件路径
    private static final String CONFIG_PATH = "src/main/resources/config.yml";

    /**
     * 加载配置（单例模式，线程安全）
     */
    public static synchronized Config load() {
        if (config != null) {
            return config;
        }

        // 检查文件是否存在
        if (!Files.exists(Paths.get(CONFIG_PATH))) {
            throw new ConfigLoadException(
                    "配置文件未找到！\n" +
                            "请在以下路径创建 config.yml：\n" +
                            CONFIG_PATH + "\n" +
                            "可参考文档创建示例配置。"
            );
        }

        // 检查是否是文件（而非目录）
        if (!Files.isRegularFile(Paths.get(CONFIG_PATH))) {
            throw new ConfigLoadException("配置路径存在，但不是一个文件：" + CONFIG_PATH);
        }

        // 加载配置
        LoaderOptions loaderOptions = new LoaderOptions();
        Constructor constructor = new Constructor(Config.class, loaderOptions);
        Yaml yaml = new Yaml(constructor);

        try {
            config = yaml.load(Files.newInputStream(Paths.get(CONFIG_PATH)));
        } catch (YAMLException e) {
            throw new ConfigLoadException("YAML 格式错误，请检查语法（缩进、冒号等）：\n" + e.getMessage(), e);
        } catch (IOException e) {
            throw new ConfigLoadException("读取配置文件时发生 IO 错误：\n" + e.getMessage(), e);
        }

        // 检查是否解析为空
        if (config == null) {
            throw new ConfigLoadException("配置文件解析结果为 null，请检查内容是否为空或格式错误。");
        }

        // 验证配置项
        validateConfig(config);

        return config;
    }

    /**
     * 获取当前已加载的配置实例，若未加载则自动调用 load()
     */
    public static Config getConfig() {
        if (config == null) {
            load();
        }
        return config;
    }

    /**
     * 验证配置的必要字段
     */
    private static void validateConfig(Config c) {
        if (c.getParatranz() == null) {
            throw new ConfigLoadException("配置错误：缺少 'paratranz' 节点");
        }
        if (c.getParatranz().getToken() == null || c.getParatranz().getToken().trim().isEmpty()) {
            throw new ConfigLoadException("配置错误：paratranz.token 不可为空");
        }
        if (c.getParatranz().getProjectId() <= 0) {
            throw new ConfigLoadException("配置错误：paratranz.projectId 必须是一个大于 0 的整数");
        }

        if (c.getGithub() == null) {
            throw new ConfigLoadException("配置错误：缺少 'github' 节点");
        }
        if (c.getGithub().getRepoOwner() == null || c.getGithub().getRepoOwner().trim().isEmpty()) {
            throw new ConfigLoadException("配置错误：github.repoOwner 不可为空");
        }
        if (c.getGithub().getRepoName() == null || c.getGithub().getRepoName().trim().isEmpty()) {
            throw new ConfigLoadException("配置错误：github.repoName 不可为空");
        }
        if (c.getGithub().getBranch() == null || c.getGithub().getBranch().trim().isEmpty()) {
            throw new ConfigLoadException("配置错误：github.branch 不可为空");
        }

        if (c.getLocal() == null) {
            throw new ConfigLoadException("配置错误：缺少 'local' 节点");
        }
        if (c.getLocal().getLanguagePath() == null || c.getLocal().getLanguagePath().trim().isEmpty()) {
            throw new ConfigLoadException("配置错误：local.languagePath 不可为空");
        }
    }
}
