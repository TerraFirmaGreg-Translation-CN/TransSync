package io.github.tfgcn.transsync;

import okhttp3.MediaType;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public final class Constants {

    private Constants() {
    }

    public static final Integer DEFAULT_PROJECT_ID = 15950;
    public static final String DEFAULT_WORKSPACE = "..";
    public static final String CONFIG_FILE = "config.json";

    public static final String SEPARATOR = "/";
    public static final String EN_US = "en_us";
    public static final String ZH_CN = "zh_cn";

    public static final String FOLDER_TOOLS_MODERN = "Tools-Modern";
    public static final String FOLDER_TOOLS_MODERN_LANGUAGE_FILES = "Tools-Modern/LanguageMerger/LanguageFiles";
    public static final String FOLDER_MODPACK_MODERN = "Modpack-Modern";

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
    public static final int WINDOW_DEFAULT_WIDTH = 960;
    public static final int WINDOW_DEFAULT_HEIGHT = 540;

    // 统计文本
    public static final String EMPTY_NAME = "未选择项目";
    public static final String EMPTY_DATE = "暂无创建时间";
    public static final String EMPTY_BADGE = "未知";
    public static final String EMPTY_STAT_VALUE = "0";
    public static final String EMPTY_STAT_TIP = "暂无统计数据";


    public static  String formatNumber(int number) {
        return String.format("%,d", number);
    }

    public static  String formatPercent(double value) {
        return String.format("%.2f%%", value * 100);
    }
}
