package io.github.transsync;

import io.github.transsync.config.Config;
import io.github.transsync.config.ConfigManager;
import io.github.transsync.exception.ConfigLoadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            Config config = ConfigManager.load();
            logger.info("配置加载成功：{}", config);
        } catch (ConfigLoadException e) {
            logger.error("配置加载失败：{}", e.getMessage(), e); // 自动打印堆栈
        } catch (Exception e) {
            logger.error("发生未预期错误", e); // 使用日志记录器输出完整堆栈
        }
    }
}
