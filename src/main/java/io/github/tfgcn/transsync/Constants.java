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
    public static final String CONFIG_FILE = "config.json";

    public static final String MSG_FAILED_CREATE_FILE = "创建配置文件失败";
    public static final String MSG_GO_GET_TOKEN = "请前往 https://paratranz.cn/users/my 点击设置获取";
    public static final String MSG_TOKEN_NOT_EXISTS = "token 为空, 请前往 https://paratranz.cn/users/my 点击设置获取，并保存到 " + CONFIG_FILE;

    public static final MediaType MULTIPART_FORM_DATA = MediaType.parse("multipart/form-data");

}
