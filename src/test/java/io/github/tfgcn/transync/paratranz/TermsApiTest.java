package io.github.tfgcn.transync.paratranz;

import io.github.tfgcn.transsync.paratranz.ParatranzApiFactory;
import io.github.tfgcn.transsync.paratranz.api.TermsApi;
import io.github.tfgcn.transsync.paratranz.model.PageResult;
import io.github.tfgcn.transsync.paratranz.model.terms.GetTermsReqDto;
import io.github.tfgcn.transsync.paratranz.model.terms.TermsDto;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static io.github.tfgcn.transync.paratranz.TestUtils.PROJECT_ID;

/**
 * desc: 术语接口测试
 *
 * @author yanmaoyuan
 */
@Slf4j
class TermsApiTest {

    static TermsApi termsApi;

    @BeforeAll
    static void init() throws IOException {
        ParatranzApiFactory paratranzApiFactory = new ParatranzApiFactory();
        termsApi = paratranzApiFactory.create(TermsApi.class);
    }

    @Test
    void testGetTerms() {
        PageResult<TermsDto> pageResult;
        try {
            GetTermsReqDto reqDto = new GetTermsReqDto();
            reqDto.setPage(1);
            reqDto.setPageSize(10);
            pageResult = termsApi.getTerms(PROJECT_ID, reqDto.asMap()).execute().body();
        } catch (IOException e) {
            log.error("error", e);
            pageResult = null;
        }
        Assertions.assertNotNull(pageResult);
        if (pageResult.getRowCount() > 0) {
            for (TermsDto termsDto : pageResult.getResults()) {
                log.info("termsDto: {}", termsDto);
            }
        }
    }
}
