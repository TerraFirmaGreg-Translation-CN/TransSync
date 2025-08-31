package io.github.tfgcn.transsync.paratranz.api;

import io.github.tfgcn.transsync.paratranz.model.PageResult;
import io.github.tfgcn.transsync.paratranz.model.strings.CreateStringsReqDto;
import io.github.tfgcn.transsync.paratranz.model.strings.StringItem;
import io.github.tfgcn.transsync.paratranz.model.strings.StringsDto;
import io.github.tfgcn.transsync.paratranz.model.strings.UpdateStringsReqDto;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.Map;

/**
 * desc: 词条相关接口
 *
 * @author yanmaoyuan
 */
public interface StringsApi {

    /**
     * 获取项目词条列表
     * @param projectId 项目ID
     * @return 词条列表
     */
    @GET("projects/{projectId}/strings")
    Call<PageResult<StringsDto>> getStrings(@Path("projectId") Integer projectId,
                                            @QueryMap Map<String, Object> params);

    /**
     * 创建项目词条
     * @param projectId 项目ID
     * @param data 词条数据
     * @return 创建结果
     */
    @POST("projects/{projectId}/strings")
    Call<StringsDto> createString(@Path("projectId") Integer projectId,
                                  @Body CreateStringsReqDto data);

    /**
     * 批量修改或删除词条
     * @param projectId 项目ID
     * @param data 词条数据
     * @return 更新结果
     */
    @PUT("projects/{projectId}/strings")
    Call<StringsDto> updateStrings(@Path("projectId") Integer projectId,
                             @Body UpdateStringsReqDto data);

    /**
     * 通过ID获取词条信息
     * @param projectId 项目ID
     * @param stringId 词条ID
     * @return 词条数据
     */
    @GET("projects/{projectId}/strings/{stringId}")
    Call<StringsDto> getString(@Path("projectId") Integer projectId,
                               @Path("stringId") Integer stringId);

    /**
     * 通过ID更新词条信息
     * @param projectId 项目ID
     * @param stringId 词条ID
     * @param data 词条数据
     * @return 更新结果
     */
    @PUT("projects/{projectId}/strings/{stringId}")
    Call<Void> updateString(@Path("projectId") Integer projectId,
                            @Path("stringId") Integer stringId,
                            @Body StringItem data);

    /**
     * 通过ID删除词条，仅管理员可用
     * @param projectId 项目ID
     * @param stringId 词条ID
     * @return 删除结果
     */
    @DELETE("projects/{projectId}/strings/{stringId}")
    Call<Void> deleteString(@Path("projectId") Integer projectId,
                            @Path("stringId") Integer stringId);
}
