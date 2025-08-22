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
}
