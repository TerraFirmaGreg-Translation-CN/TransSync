package io.github.tfgcn.transsync;

import io.github.tfgcn.transsync.paratranz.interceptor.LoggingInterceptor;
import okhttp3.MediaType;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public final class Constants {

    private Constants() {
    }

    public static final Integer DEFAULT_PROJECT_ID = -1;
    public static final String DEFAULT_WORKSPACE = "..";
    public static final String DEFAULT_HTTP_LOG_LEVEL = LoggingInterceptor.Level.NONE.name();
    public static final String CONFIG_FILE = "config.json";

    // Environments
    public static final String ENV_PARATRANZ_TOKEN = "PARATRANZ_TOKEN";
    public static final String ENV_PARATRANZ_PROJECT_ID = "PARATRANZ_PROJECT_ID";
    public static final String ENV_PARATRANZ_WORKSPACE = "PARATRANZ_WORKSPACE";
    public static final String ENV_HTTP_LOG_LEVEL = "HTTP_LOG_LEVEL";

    public static final String SEPARATOR = "/";
    public static final String EN_US = "en_us";
    public static final String ZH_CN = "zh_cn";

    public static final String MSG_FOLDER_NOT_FOUND = "找不到目录";
    public static final String MSG_FOLDER_INVALID = "非法目录";
    public static final String MSG_FAILED_CREATE_FILE = "创建配置文件失败";
    public static final String MSG_GO_GET_TOKEN = "请前往 https://paratranz.cn/users/my 点击设置获取";
    public static final String MSG_TOKEN_NOT_EXISTS = "token未正确设置, 请前往 https://paratranz.cn/users/my 点击设置获取";

    public static final MediaType MULTIPART_FORM_DATA = MediaType.parse("multipart/form-data");

    public static final String TITLE = "TFG汉化同步工具";
    public static final String VERSION = "1.0.0";
    public static final String DESCRIPTION = "Toolkit for sync TerraFirmaGreg Chinese translation with paratrans.cn";
    public static final int WINDOW_MIN_WIDTH = 640;
    public static final int WINDOW_MIN_HEIGHT = 500;
    public static final int WINDOW_DEFAULT_WIDTH = 1080;
    public static final int WINDOW_DEFAULT_HEIGHT = 720;

    // 统计文本
    public static final String EMPTY_NAME = "未选择项目";
    public static final String EMPTY_DATE = "暂无创建时间";
    public static final String EMPTY_BADGE = "未知";
    public static final String EMPTY_STAT_VALUE = "0";
    public static final String EMPTY_STAT_TIP = "暂无统计数据";

    public static final String ENSURE_FORCE_TITLE = "是否强制覆盖未翻译文本?";
    public static final String ENSURE_FORCE_MESSAGE =
            "当检测到本地译文未翻译，而 paratranz 项目将其标记为'已翻译'时，是否覆盖该词条？" +
            "选择'是'将强制把该词条重置为'未翻译'状态，选择'否'将跳过该词条。";

    public static String formatNumber(int number) {
        return String.format("%,d", number);
    }

    public static String formatPercent(double value) {
        return String.format("%.2f%%", value * 100);
    }

    public static String formatFileSize(int size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1048576) {
            return size / 1024 + " KiB";
        } else if (size < 1073741824) {
            return size / 1048576 + " MiB";
        } else {
            return size / 1073741824 + " GiB";
        }
    }
}