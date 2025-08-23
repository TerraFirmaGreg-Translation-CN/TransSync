package io.github.tfgcn.transsync.paratranz.error;

import lombok.Data;

/**
 * desc:
 *
 * @author: yanmaoyuan
 */
@Data
public class ApiError {
    private String message;// 错误消息
    private Integer code;// 5位错误代码，注意与下面的HTTP状态码区分，部分接口不返回
    /*
     * 400 - 调用参数错误
     * 401 - Token 错误或过期
     * 403 - 没有相关权限
     * 404 - 资源不存在
     * 405 - 没有相关HTTP方法，一般为调用方法错误
     * 429 - 调用过于频繁，具体频率限制请看上一节
     * 500 - 服务器错误，一般会提供具体出错的位置，请发送给站长方便定位问题
     * 502 - 服务器无响应，部分用户被墙时可能会遇到
     * 503 - 服务不可用
     * 504 - 服务超时，访问量大时会出现
     */
}
