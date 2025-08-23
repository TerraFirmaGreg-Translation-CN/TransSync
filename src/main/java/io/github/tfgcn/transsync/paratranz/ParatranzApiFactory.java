package io.github.tfgcn.transsync.paratranz;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.tfgcn.transsync.Config;
import io.github.tfgcn.transsync.paratranz.error.ErrorHandlingCallAdapterFactory;
import io.github.tfgcn.transsync.paratranz.interceptor.AuthInterceptor;
import io.github.tfgcn.transsync.paratranz.interceptor.LoggingInterceptor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;

import static io.github.tfgcn.transsync.Constants.*;

/**
 * desc: paratranz 接口工厂
 *
 * @author yanmaoyuan
 */
@Slf4j
public class ParatranzApiFactory {

    private final Retrofit retrofit;

    private final AuthInterceptor authInterceptor;
    private final LoggingInterceptor loggingInterceptor;

    public ParatranzApiFactory() throws IOException {
        // 从配置文件中读取token
        Config config = Config.getInstance();
        String token = config.getToken();

        if (StringUtils.isBlank(token) || token.length() != 32) {
            throw new RuntimeException(MSG_TOKEN_NOT_EXISTS);
        }

        LoggingInterceptor.Level logLevel;
        if (StringUtils.isBlank(config.getHttpLogLevel())) {
            logLevel = LoggingInterceptor.Level.NONE;
        } else {
            logLevel = LoggingInterceptor.Level.valueOf(config.getHttpLogLevel().toUpperCase().trim());
        }

        // jackson 对象映射器
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        authInterceptor = new AuthInterceptor(token);
        loggingInterceptor = new LoggingInterceptor(logLevel);

        // 创建 OkHttpClient 并添加认证拦截器
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl("https://paratranz.cn/api/")
                .client(okHttpClient)
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .addCallAdapterFactory(new ErrorHandlingCallAdapterFactory())
                .build();
    }

    public ParatranzApiFactory(Config config) {
        String token = config.getToken();

        if (StringUtils.isBlank(token) || token.length() != 32) {
            throw new RuntimeException(MSG_TOKEN_NOT_EXISTS);
        }

        LoggingInterceptor.Level logLevel;
        if (StringUtils.isBlank(config.getHttpLogLevel())) {
            logLevel = LoggingInterceptor.Level.NONE;
        } else {
            logLevel = LoggingInterceptor.Level.valueOf(config.getHttpLogLevel().toUpperCase().trim());
        }

        // jackson 对象映射器
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        authInterceptor = new AuthInterceptor(token);
        loggingInterceptor = new LoggingInterceptor(logLevel);

        // 创建 OkHttpClient 并添加认证拦截器
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl("https://paratranz.cn/api/")
                .client(okHttpClient)
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .addCallAdapterFactory(new ErrorHandlingCallAdapterFactory())
                .build();
    }

    public void setToken(String token) {
        if (StringUtils.isBlank(token) || token.length() != 32) {
            throw new RuntimeException(MSG_TOKEN_NOT_EXISTS);
        }

        authInterceptor.setToken(token);
    }

    public void setHttpLogLevel(LoggingInterceptor.Level logLevel) {
        loggingInterceptor.setLevel(logLevel);
    }

    public <T> T create(Class<T> service) {
        return retrofit.create(service);
    }
}