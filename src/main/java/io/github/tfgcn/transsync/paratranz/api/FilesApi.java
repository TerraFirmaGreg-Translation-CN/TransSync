package io.github.tfgcn.transsync.paratranz.api;

import io.github.tfgcn.transsync.paratranz.model.files.FilesDto;
import io.github.tfgcn.transsync.paratranz.model.files.UploadFileResp;
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
    @GET("projects/{projectId}/files")
    Call<List<FilesDto>> getFiles(@Path("projectId") int projectId);

    @Multipart
    @POST("projects/{projectId}/files")
    Call<UploadFileResp> uploadFile(@Path("projectId") int projectId,
                                    @Part("path") RequestBody path,
                                    @Part MultipartBody.Part file);

    /**
     * <p>通过ID上传并更新文件。</p>
     *
     * <p>注意此接口仅更新原文，不对译文做改动， 更新译文请用下方的更新文件接口 <code>POST /projects/{projectId}/files/{fileId}/translation</code></p>
     *
     * @param projectId
     * @param fileId
     * @param file
     * @return
     */
    @Multipart
    @POST("projects/{projectId}/files/{fileId}")
    Call<UploadFileResp> updateFile(@Path("projectId") int projectId,
                                    @Path("fileId") int fileId,
                                    @Part MultipartBody.Part file);
}
