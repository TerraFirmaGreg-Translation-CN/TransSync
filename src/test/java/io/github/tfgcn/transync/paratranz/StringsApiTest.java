package io.github.tfgcn.transync.paratranz;

import io.github.tfgcn.transsync.paratranz.ParatranzApiFactory;
import io.github.tfgcn.transsync.paratranz.api.StringsApi;
import io.github.tfgcn.transsync.paratranz.model.PageResult;
import io.github.tfgcn.transsync.paratranz.model.strings.GetStringsReqDto;
import io.github.tfgcn.transsync.paratranz.model.strings.StringsDto;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import retrofit2.Response;

import java.io.IOException;
import java.util.Collections;

import static io.github.tfgcn.transync.paratranz.TestUtils.*;
/**
 * desc: 词条接口测试
 *
 * @author yanmaoyuan
 */
@Slf4j
public class StringsApiTest {

    static StringsApi stringsApi;

    @BeforeAll
    public static void beforeAll() throws IOException {
        ParatranzApiFactory paratranzApiFactory = new ParatranzApiFactory();
        stringsApi = paratranzApiFactory.create(StringsApi.class);
    }

    @Test
    void testGetStrings() throws IOException {
        Response<PageResult<StringsDto>> response = stringsApi.getStrings(PROJECT_ID, Collections.emptyMap()).execute();
        Assertions.assertTrue(response.isSuccessful());
        Assertions.assertNotNull(response.body());
        PageResult<StringsDto> pageResult = response.body();
        if (pageResult.getRowCount() > 0) {
            Assertions.assertFalse(pageResult.getResults().isEmpty());
            log.info("getStrings, size:{}", pageResult.getRowCount());
        } else {
            Assertions.assertTrue(pageResult.getResults().isEmpty());
            log.info("getStrings: no data");
        }
    }

    @Test
    void getGetStringsByFileId() throws IOException {
        Integer fileId = getTestFileId();

        GetStringsReqDto reqDto = new GetStringsReqDto();
        reqDto.setFile(fileId);

        Response<PageResult<StringsDto>> response = stringsApi.getStrings(PROJECT_ID, reqDto.asMap()).execute();
        Assertions.assertTrue(response.isSuccessful());
        Assertions.assertNotNull(response.body());
        PageResult<StringsDto> pageResult = response.body();
        if (pageResult.getRowCount() > 0) {
            Assertions.assertFalse(pageResult.getResults().isEmpty());
            for (StringsDto item : pageResult.getResults()) {
                log.info("getString: {}", item);
            }
        } else {
            Assertions.assertTrue(pageResult.getResults().isEmpty());
        }
    }

    @Test
    void testCreateString() throws IOException {
        StringsDto reqDto = new StringsDto();
        reqDto.setKey("test_key");
        reqDto.setOriginal("test_original");
        reqDto.setTranslation("test_translation");
        reqDto.setFileId(getTestFileId());
    }
}