package io.github.tfgcn.transsync.paratranz.api;

import io.github.tfgcn.transsync.paratranz.model.PageResult;
import io.github.tfgcn.transsync.paratranz.model.terms.CreateTermsReqDto;
import io.github.tfgcn.transsync.paratranz.model.terms.TermsDto;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.Map;

/**
 * desc: 术语相关接口
 *
 * @author yanmaoyuan
 */
public interface TermsApi {

    /**
     * 获取项目术语列表
     * @param projectId 项目ID
     * @return 术语列表
     */
    @GET("projects/{projectId}/terms")
    Call<PageResult<TermsDto>> getTerms(@Path("projectId") Integer projectId, @QueryMap Map<String, Object> params);

    /**
     * 创建新术语，如果已存在相同术语会失败
     * @param projectId 项目ID
     * @param data 创建参数
     * @return 创建结果
     */
    @POST("projects/{projectId}/terms")
    Call<TermsDto> createTerm(@Path("projectId") Integer projectId, @Body CreateTermsReqDto data);
}
