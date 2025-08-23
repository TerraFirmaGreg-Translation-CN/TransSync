package io.github.tfgcn.transsync.paratranz.api;

import io.github.tfgcn.transsync.paratranz.model.files.FilesDto;
import io.github.tfgcn.transsync.paratranz.model.files.TranslationDto;
import io.github.tfgcn.transsync.paratranz.model.files.FileUploadRespDto;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public interface FilesApi {
    /**
     * 获取项目下的所有文件
     * @param projectId 项目ID
     * @return 文件列表
     */
    @GET("projects/{projectId}/files")
    Call<List<FilesDto>> getFiles(@Path("projectId") Integer projectId);

    /**
     * 上传文件
     * @param projectId 项目ID
     * @param path 路径
     * @param file 文件
     * @return 上传结果
     */
    @Multipart
    @POST("projects/{projectId}/files")
    Call<FileUploadRespDto> uploadFile(@Path("projectId") Integer projectId,
                                       @Part("path") RequestBody path,
                                       @Part MultipartBody.Part file);

    /**
     * 通过ID上传并更新文件。
     * <p>注意此接口仅更新原文，不对译文做改动， 更新译文请用下方的更新文件接口 <code>POST /projects/{projectId}/files/{fileId}/translation</code></p>
     * @param projectId 项目ID
     * @param fileId 文件ID
     * @param file 文件
     * @return 上传结果
     */
    @Multipart
    @POST("projects/{projectId}/files/{fileId}")
    Call<FileUploadRespDto> updateFile(@Path("projectId") Integer projectId,
                                       @Path("fileId") Integer fileId,
                                       @Part MultipartBody.Part file);

    /**
     * 通过ID获取文件翻译数据
     * @param projectId 项目ID
     * @param fileId 文件ID
     * @return 返回翻译过的词条
     */
    @GET("projects/{projectId}/files/{fileId}/translation")
    Call<List<TranslationDto>> getTranslate(@Path("projectId") Integer projectId,
                                            @Path("fileId") Integer fileId);

    /**
     * 通过ID上传并更新文件中的词条翻译。
     * <p>注意此接口仅更新译文，不对原文做改动</p>
     * @param projectId 项目ID
     * @param fileId 文件ID
     * @param file 文件数据，格式需与创建时的文件保持一致，也可上传标准JSON格式（文件名需为原文件名加.json）
     * @param force 是否强制覆盖翻译，默认为 false，此时翻译仅会覆盖未人工编辑过的词条
     * @return 上传结果
     */
    @Multipart
    @POST("projects/{projectId}/files/{fileId}/translation")
    Call<FileUploadRespDto> updateTranslate(@Path("projectId") Integer projectId,
                                            @Path("fileId") Integer fileId,
                                            @Part MultipartBody.Part file,
                                            @Part("force") Boolean force);
}