package io.github.tfgcn.transsync.paratranz.error;

import okhttp3.Request;
import okio.Timeout;
import retrofit2.*;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * desc: 统一错误处理
 * <p>当API不能正常返回HTTP状态 200 时，自动将其结果转为 ApiError 并抛出异常。</p>
 * @author yanmaoyuan
 * @see ApiError
 * @see ApiException
 */
public class ErrorHandlingCallAdapterFactory extends CallAdapter.Factory {
    @Override
    public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        // 检查返回类型是否是Call<T>
        if (getRawType(returnType) != Call.class) {
            return null;
        }
        
        // 提取泛型参数
        Type responseType = getParameterUpperBound(0, (ParameterizedType) returnType);
        
        return new CallAdapter<Object, Call<?>>() {
            @Override
            public Type responseType() {
                return responseType;
            }
            
            @Override
            public Call<Object> adapt(Call<Object> call) {
                return new ErrorHandlingCall<>(call);
            }
        };
    }
    
    private static class ErrorHandlingCall<T> implements Call<T> {
        private final Call<T> delegate;
        
        public ErrorHandlingCall(Call<T> delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public Response<T> execute() throws IOException {
            Response<T> response = delegate.execute();
            if (!response.isSuccessful()) {
                // 统一处理错误
                handleError(response);
            }
            return response;
        }
        
        @Override
        public void enqueue(Callback<T> callback) {
            delegate.enqueue(new Callback<T>() {
                @Override
                public void onResponse(Call<T> call, Response<T> response) {
                    if (response.isSuccessful()) {
                        callback.onResponse(call, response);
                    } else {
                        // 统一处理错误
                        try {
                            handleError(response);
                        } catch (IOException e) {
                            callback.onFailure(call, e);
                        }
                    }
                }
                
                @Override
                public void onFailure(Call<T> call, Throwable t) {
                    callback.onFailure(call, t);
                }
            });
        }
        
        private void handleError(Response<T> response) throws IOException {
            // 解析错误信息
            ApiError apiError = ErrorParser.parseError(response);
            if (apiError != null) {
                throw new ApiException(apiError.getMessage(), apiError.getCode());
            } else {
                throw new ApiException("请求失败: " + response.code(), response.code());
            }
        }
        
        // 其他Call接口方法的实现...
        @Override
        public boolean isExecuted() {
            return delegate.isExecuted();
        }
        
        @Override
        public void cancel() {
            delegate.cancel();
        }
        
        @Override
        public boolean isCanceled() {
            return delegate.isCanceled();
        }
        
        @Override
        public Call<T> clone() {
            return new ErrorHandlingCall<>(delegate.clone());
        }
        
        @Override
        public Request request() {
            return delegate.request();
        }
        
        @Override
        public Timeout timeout() {
            return delegate.timeout();
        }
    }
}