package io.github.tfgcn.transync.paratranz;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.tfgcn.transsync.Constants;
import io.github.tfgcn.transsync.paratranz.ParatranzApiFactory;
import io.github.tfgcn.transsync.paratranz.api.FilesApi;
import io.github.tfgcn.transsync.paratranz.api.StringsApi;
import io.github.tfgcn.transsync.paratranz.model.StageEnum;
import io.github.tfgcn.transsync.paratranz.model.files.FileUpdateReqDto;
import io.github.tfgcn.transsync.paratranz.model.files.FilesDto;
import io.github.tfgcn.transsync.paratranz.model.files.TranslationDto;
import io.github.tfgcn.transsync.paratranz.model.files.FileUploadRespDto;
import io.github.tfgcn.transsync.paratranz.model.strings.StringItem;
import io.github.tfgcn.transsync.paratranz.model.strings.StringsDto;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import retrofit2.Call;
import retrofit2.Response;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static io.github.tfgcn.transync.paratranz.TestUtils.*;

/**
 * desc: 文件接口测试
 *
 * @author yanmaoyuan
 */
@Slf4j
class FilesApiTest {

    static ParatranzApiFactory paratranzApiFactory;
    static FilesApi filesApi;

    @BeforeAll
    static void before() throws IOException {
        paratranzApiFactory = new ParatranzApiFactory();
        filesApi = paratranzApiFactory.create(FilesApi.class);
    }

    @Test
    void testGetFiles() throws IOException {
        Call<List<FilesDto>> call = filesApi.getFiles(PROJECT_ID);
        Response<List<FilesDto>> response = call.execute();
        Assertions.assertTrue(response.isSuccessful());
    }

    @Test
    void testUploadFile() throws IOException {
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
        File file = getOrCreateTestFile(TEST_EN_FILE, TEST_EN_CONTENT);

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
                    RequestBody.create(Constants.MULTIPART_FORM_DATA, file));

            Response<FileUploadRespDto> resp = filesApi.updateFile(PROJECT_ID, fileId, filePart).execute();
            Assertions.assertTrue(resp.isSuccessful());
            Assertions.assertNotNull(resp.body());
            Assertions.assertNotNull(resp.body().getFile());
            Assertions.assertNotNull(resp.body().getRevision());
            log.info("{}", resp.body());
        } else {
            // 注意，上传的文件名采用 TEST_FILE
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", TEST_FILE,
                    RequestBody.create(Constants.MULTIPART_FORM_DATA, file));

            // 创建描述部分
            RequestBody pathPart = RequestBody.create(Constants.MULTIPART_FORM_DATA, TEST_FOLDER);

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
    void updateFileInfo() throws IOException {
        Integer fileId = getTestFileId();

        FileUpdateReqDto reqDto = new FileUpdateReqDto();
        reqDto.setName("test/zh_cn.json");

        Response<FilesDto> response = filesApi.updateFileInfo(PROJECT_ID, fileId, reqDto).execute();
        Assertions.assertTrue(response.isSuccessful());
    }

    @Test
    void testDeleteFile() throws IOException {
        Integer fileId = getTestFileId();
        Response<Void> response = filesApi.deleteFile(PROJECT_ID, fileId).execute();
        Assertions.assertTrue(response.isSuccessful());
        log.info("{}", response);
        deleteTestFileId();
    }

    @Test
    void testGetTranslate() throws IOException {
        Integer fileId = getTestFileId();

        Response<List<TranslationDto>> response = filesApi.getTranslate(PROJECT_ID, fileId).execute();
        Assertions.assertTrue(response.isSuccessful());
        Assertions.assertNotNull(response.body());
        log.info("{}", response.body());

        StringsApi stringsApi = paratranzApiFactory.create(StringsApi.class);
        for (TranslationDto translation : response.body()) {
            log.info("{}", translation);
            Response<StringsDto> getStringResp = stringsApi.getString(PROJECT_ID, translation.getId()).execute();
            Assertions.assertTrue(getStringResp.isSuccessful());
            Assertions.assertNotNull(getStringResp.body());
            log.info("{}", getStringResp.body());
        }
    }

    @Test
    void testUpdateTranslate() throws IOException {
        Integer fileId = getTestFileId();

        File file = getOrCreateTestFile(TEST_ZH_FILE, TEST_ZH_CONTENT);
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", TEST_FILE,
                RequestBody.create(Constants.MULTIPART_FORM_DATA, file));
        Response<FileUploadRespDto> updateResp = filesApi.updateTranslate(PROJECT_ID, fileId, filePart, false).execute();
        Assertions.assertTrue(updateResp.isSuccessful());
        Assertions.assertNotNull(updateResp.body());

        log.info("{}", updateResp.body());
        // 注意，这个接口只会上传译文，不会修改词条的翻译状态。
        // 更新词条翻译状态需要使用 POST projects/{project_id}/strings/{string_id}
    }

    @Test
    void testUpdateTranslateStrings() throws IOException {
        Integer fileId = getTestFileId();
        StringsApi stringsApi = paratranzApiFactory.create(StringsApi.class);

        // 读取远程翻译内容
        Response<List<TranslationDto>> getTranslateResp = filesApi.getTranslate(PROJECT_ID, fileId).execute();
        Assertions.assertTrue(getTranslateResp.isSuccessful());
        Assertions.assertNotNull(getTranslateResp.body());
        List<TranslationDto> translations = getTranslateResp.body();
        log.info("{}", translations);

        // 读取本地汉化文件
        File file = getOrCreateTestFile(TEST_ZH_FILE, TEST_ZH_CONTENT);
        Map<String, String> map;

        ObjectMapper objectMapper = new ObjectMapper();
        map = objectMapper.readValue(file, new TypeReference<>() {});

        // 遍历原文，上传翻译文件
        for (TranslationDto translation : translations) {
            String key = translation.getKey();
            String value = map.get(key);
            if (StringUtils.isNotBlank(value)) {
                if (value.equals(translation.getTranslation())) {
                    log.info("词条未修改, key:{}, value:{}", key, value);
                } else {
                    log.info("更新词条, key:{} , value:{} -> {}", key, translation.getTranslation(), value);
                    StringItem stringItem = new StringItem();
                    stringItem.setKey(key);
                    stringItem.setOriginal(translation.getOriginal());
                    stringItem.setTranslation(value);
                    stringItem.setStage(StageEnum.TRANSLATED.getValue());
                    stringItem.setContext(translation.getContext());

                    Response<Void> updateStringResponse = stringsApi.updateString(PROJECT_ID, translation.getId(), stringItem).execute();
                    Assertions.assertTrue(updateStringResponse.isSuccessful());
                    log.info("{}", updateStringResponse.body());
                }
            }
        }
    }
}