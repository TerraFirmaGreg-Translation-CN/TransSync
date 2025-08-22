package io.github.tfgcn.transync.paratranz;

import io.github.tfgcn.transsync.Constants;
import io.github.tfgcn.transsync.paratranz.ApiFactory;
import io.github.tfgcn.transsync.paratranz.api.FilesApi;
import io.github.tfgcn.transsync.paratranz.model.files.FilesDto;
import io.github.tfgcn.transsync.paratranz.model.files.UploadFileResp;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import retrofit2.Call;
import retrofit2.Response;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
@Slf4j
class FilesApiTest {

    @Test
    void testGetFiles() throws IOException {
        ApiFactory apiFactory = new ApiFactory();
        FilesApi filesApi = apiFactory.create(FilesApi.class);

        Call<List<FilesDto>> call = filesApi.getFiles(15950);
        Response<List<FilesDto>> response = call.execute();
        Assertions.assertTrue(response.isSuccessful());
    }

    @Test
    void testUploadFile() throws IOException {
        ApiFactory apiFactory = new ApiFactory();
        FilesApi filesApi = apiFactory.create(FilesApi.class);

        // 文件
        File file = new File("pom.xml");

        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(),
                RequestBody.create(file, Constants.MULTIPART_FORM_DATA));

        // 创建描述部分
        String path = "/tmp";
        RequestBody pathPart = RequestBody.create(path, Constants.MULTIPART_FORM_DATA);

        UploadFileResp resp = filesApi.uploadFile(15950, pathPart, filePart).execute().body();
        log.info("{}", resp);
    }
}