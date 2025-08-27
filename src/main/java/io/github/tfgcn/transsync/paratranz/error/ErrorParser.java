package io.github.tfgcn.transsync.paratranz.error;

import io.github.tfgcn.transsync.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import retrofit2.Response;

import java.io.IOException;

@Slf4j
public class ErrorParser {

    /**
     * 解析错误响应为通用ApiError对象
     */
    public static ApiError parseError(Response<?> response) {
        try (ResponseBody errorBody = response.errorBody()) {
            if (errorBody != null) {
                return JsonUtils.fromJson(errorBody.string(), ApiError.class);
            }
        } catch (IOException e) {
            // 日志记录或处理异常
            log.error("Error parsing error response failed.", e);
        }
        return null;
    }
    
    /**
     * 解析错误响应为特定ApiError对象
     */
    public static <T> T parseError(Response<?> response, Class<T> errorClass) {
        try (ResponseBody errorBody = response.errorBody()) {
            if (errorBody != null) {
                return JsonUtils.fromJson(errorBody.string(), errorClass);
            }
        } catch (IOException e) {
            // 日志记录或处理异常
            log.error("Error parsing error response failed.", e);
        }
        return null;
    }
    
    /**
     * 获取错误响应的原始字符串
     */
    public static String getErrorString(Response<?> response) {
        try (ResponseBody errorBody = response.errorBody()) {
            if (errorBody != null) {
                return errorBody.string();
            }
        } catch (IOException e) {
            // 日志记录或处理异常
            log.error("Error parsing error response failed.", e);
        }
        return null;
    }
}