package io.github.tfgcn.transsync.paratranz.interceptor;

import okhttp3.*;
import okio.Buffer;
import okio.BufferedSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class LoggingInterceptor implements Interceptor {
    private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);
    
    // 日志级别枚举
    public enum Level {
        NONE,       // 不记录日志
        BASIC,      // 记录请求和响应基本信息
        HEADERS,    // 记录请求和响应头信息
        BODY        // 记录所有信息，包括请求和响应体
    }
    
    private volatile Level level = Level.NONE;
    
    public LoggingInterceptor() {
        // 默认构造函数
    }
    
    public LoggingInterceptor(Level level) {
        this.level = level;
    }
    
    public void setLevel(Level level) {
        this.level = level;
    }
    
    public Level getLevel() {
        return level;
    }
    
    @Override
    public Response intercept(Chain chain) throws IOException {
        // 如果日志级别为NONE，直接执行请求
        if (level == Level.NONE) {
            return chain.proceed(chain.request());
        }
        
        Request request = chain.request();
        
        // 记录请求信息
        logRequest(request);
        
        long startNs = System.nanoTime();
        Response response;
        try {
            response = chain.proceed(request);
        } catch (Exception e) {
            logger.error("<-- HTTP FAILED: {}", e.getMessage());
            throw e;
        }
        
        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
        
        // 记录响应信息
        logResponse(response, tookMs);
        
        return response;
    }
    
    private void logRequest(Request request) throws IOException {
        if (logger.isInfoEnabled()) {
            logger.info("--> {} {}", request.method(), request.url());
        }

        if (level == Level.HEADERS || level == Level.BODY) {
            Headers headers = request.headers();
            for (int i = 0, count = headers.size(); i < count; i++) {
                String name = headers.name(i);
                // 敏感信息（如认证头）进行脱敏处理
                String value = "Authorization".equalsIgnoreCase(name) ? "****" : headers.value(i);
                logger.debug("{}: {}", name, value);
            }

            if (level == Level.BODY && request.body() != null) {
                RequestBody requestBody = request.body();
                Buffer buffer = new Buffer();
                requestBody.writeTo(buffer);

                String contentType = requestBody.contentType() != null ?
                    requestBody.contentType().toString() : "unknown";

                if (isTextBased(contentType)) {
                    logger.debug("Request Body: {}", buffer.readString(StandardCharsets.UTF_8));
                } else {
                    logger.debug("Request Body: <binary content, {} bytes>", requestBody.contentLength());
                }
            }
        }
    }
    
    private void logResponse(Response response, long tookMs) throws IOException {
        if (!logger.isInfoEnabled()) {
            return;
        }
        
        ResponseBody responseBody = response.body();
        long contentLength = responseBody != null ? responseBody.contentLength() : -1;
        
        logger.info("<-- {} {} {} ({}ms)", 
                   response.code(), 
                   response.message(), 
                   response.request().url(),
                   tookMs);
        
        if (level == Level.HEADERS || level == Level.BODY) {
            Headers headers = response.headers();
            for (int i = 0, count = headers.size(); i < count; i++) {
                logger.info("{}: {}", headers.name(i), headers.value(i));
            }
            
            if (level == Level.BODY && responseBody != null) {
                // 复制响应体以便可以多次读取
                BufferedSource source = responseBody.source();
                source.request(Long.MAX_VALUE); // 缓冲整个响应体
                Buffer buffer = source.getBuffer();
                
                String contentType = responseBody.contentType() != null ? 
                    responseBody.contentType().toString() : "unknown";
                
                if (isTextBased(contentType)) {
                    String content = buffer.clone().readString(StandardCharsets.UTF_8);
                    logger.info("Response Body: {}", content);
                } else {
                    logger.info("Response Body: <binary content, {} bytes>", contentLength);
                }
            }
        }
    }
    
    /**
     * 判断内容类型是否为文本类型
     */
    private boolean isTextBased(String contentType) {
        if (contentType == null) {
            return false;
        }
        
        return contentType.startsWith("text/") ||
               contentType.contains("json") ||
               contentType.contains("xml") ||
               contentType.contains("form") ||
               contentType.contains("javascript") ||
               contentType.contains("x-www-form-urlencoded");
    }
}