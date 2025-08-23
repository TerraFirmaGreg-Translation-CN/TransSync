package io.github.tfgcn.transync.paratranz;

import io.github.tfgcn.transsync.Constants;
import io.github.tfgcn.transsync.paratranz.ApiFactory;
import io.github.tfgcn.transsync.paratranz.api.FilesApi;
import io.github.tfgcn.transsync.paratranz.model.files.FilesDto;
import io.github.tfgcn.transsync.paratranz.model.files.TranslationDto;
import io.github.tfgcn.transsync.paratranz.model.files.FileUploadRespDto;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import retrofit2.Call;
import retrofit2.Response;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
@Slf4j
class FilesApiTest {

    static final Integer PROJECT_ID = Constants.DEFAULT_PROJECT_ID;// change this if you want to test with other projects

    static final String TEST_FOLDER = "test";
    static final String TEST_FILE = "test/zh_cn.json";

    static final String TEST_EN_FILE = "test/en_us.json";
    static final String TEST_EN_CONTENT = "{\"key\":\"Hello\"}";
    static final String TEST_ZH_FILE = "test/zh_cn.json";
    static final String TEST_ZH_CONTENT = "{\"key\":\"你好\"}";
    static final String TEST_FILE_ID_NAME = "test/file_id.txt";

    @Test
    void testGetFiles() throws IOException {
        ApiFactory apiFactory = new ApiFactory();
        FilesApi filesApi = apiFactory.create(FilesApi.class);

        Call<List<FilesDto>> call = filesApi.getFiles(PROJECT_ID);
        Response<List<FilesDto>> response = call.execute();
        Assertions.assertTrue(response.isSuccessful());
    }

    @Test
    void testUploadFile() throws IOException {
        ApiFactory apiFactory = new ApiFactory();
        FilesApi filesApi = apiFactory.create(FilesApi.class);

        // 扫描项目文件，判断是否存在同名文件
        Response<List<FilesDto>> fileListResponse = filesApi.getFiles(PROJECT_ID).execute();
        Assertions.assertTrue(fileListResponse.isSuccessful());
        List<FilesDto> fileList = fileListResponse.body();

        FilesDto existedFile = null;
        if (CollectionUtils.isNotEmpty(fileList)) {
            for (FilesDto file : fileList) {
                if (file.getName().equals(TEST_FILE)) {
                    existedFile = file;
                    log.info("existedFile: {}", file);
                    break;
                }
            }
        }

        // 测试文件
        File file = getOrCreate(TEST_EN_FILE, TEST_EN_CONTENT);

        if (existedFile != null) {
            // 文件ID
            Integer fileId = existedFile.getId();
            saveTestFileId(fileId);

            // 判断文件指纹
            try (FileInputStream fis = new FileInputStream(file)) {
                String md5 = DigestUtils.md5Hex(fis);
                if (md5.equals(existedFile.getHash())) {// 手动修改 test/en_us.json 内容，看看md5的比较结果
                    log.info("文件未修改，跳过");
                    return;
                }
            }

            // 更新文件
            log.info("更新文件：{}", TEST_FILE);
            // 注意，上传的文件名采用 TEST_FILE
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", TEST_FILE,
                    RequestBody.create(file, Constants.MULTIPART_FORM_DATA));

            Response<FileUploadRespDto> resp = filesApi.updateFile(PROJECT_ID, fileId, filePart).execute();
            Assertions.assertTrue(resp.isSuccessful());
            Assertions.assertNotNull(resp.body());
            Assertions.assertNotNull(resp.body().getFile());
            Assertions.assertNotNull(resp.body().getRevision());
            log.info("{}", resp.body());
        } else {
            // 注意，上传的文件名采用 TEST_FILE
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", TEST_FILE,
                    RequestBody.create(file, Constants.MULTIPART_FORM_DATA));

            // 创建描述部分
            RequestBody pathPart = RequestBody.create(TEST_FOLDER, Constants.MULTIPART_FORM_DATA);

            Response<FileUploadRespDto> resp = filesApi.uploadFile(PROJECT_ID, pathPart, filePart).execute();

            Assertions.assertTrue(resp.isSuccessful());
            Assertions.assertNotNull(resp.body());
            Assertions.assertNotNull(resp.body().getFile());
            Assertions.assertNotNull(resp.body().getRevision());

            Integer fileId = resp.body().getFile().getId();
            saveTestFileId(fileId);
        }
    }

    @Test
    void testGetTranslate() throws IOException {
        Integer fileId = getTestFileId();

        ApiFactory apiFactory = new ApiFactory();
        FilesApi filesApi = apiFactory.create(FilesApi.class);

        Response<List<TranslationDto>> response = filesApi.getTranslate(PROJECT_ID, fileId).execute();
        Assertions.assertTrue(response.isSuccessful());
        log.info("{}", response.body());
    }

    @Test
    void testUpdateTranslate() throws IOException {
        Integer fileId = getTestFileId();

        ApiFactory apiFactory = new ApiFactory();
        FilesApi filesApi = apiFactory.create(FilesApi.class);
        File file = getOrCreate(TEST_ZH_FILE, TEST_ZH_CONTENT);
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", TEST_FILE,
                RequestBody.create(file, Constants.MULTIPART_FORM_DATA));
        Response<FileUploadRespDto> updateResp = filesApi.updateTranslate(PROJECT_ID, fileId, filePart, false).execute();
        Assertions.assertTrue(updateResp.isSuccessful());
    }

    private void saveTestFileId(Integer fileId) throws IOException {
        File file = new File(TEST_FILE_ID_NAME);
        if (file.exists()) {
            String oldFileId = FileUtils.readFileToString(file, "UTF-8");
            if (oldFileId.equals(fileId.toString())) {
                return;
            }
        }

        log.info("saveTestFileId: {}", fileId);
        file.getParentFile().mkdirs();
        FileUtils.writeStringToFile(file, fileId.toString(), "UTF-8");
    }

    private Integer getTestFileId() throws IOException {
        File file = new File(TEST_FILE_ID_NAME);
        if (!file.exists()) {
            throw new IOException("File id not exists, please run testUploadFile() first.");
        }
        try {
            return Integer.parseInt(FileUtils.readFileToString(file, "UTF-8"));
        } catch (NumberFormatException e) {
            throw new IOException("Invalid file id: " + file.getAbsolutePath());
        }
    }

    /**
     * 获取测试文件。
     * 若不存在则自动生成
     * @return
     */
    private File getOrCreate(String filename, String content) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
            FileUtils.writeStringToFile(file, content, "UTF-8");
        }
        return file;
    }
}