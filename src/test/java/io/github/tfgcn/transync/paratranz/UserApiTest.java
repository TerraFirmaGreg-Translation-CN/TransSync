package io.github.tfgcn.transync.paratranz;

import io.github.tfgcn.transsync.paratranz.ParatranzApiFactory;
import io.github.tfgcn.transsync.paratranz.api.UsersApi;
import io.github.tfgcn.transsync.paratranz.model.users.UsersDto;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import retrofit2.Response;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
@Slf4j
public class UserApiTest {

    @Test
    void testGetMy() throws Exception {
        // 这个接口必须token有效才能使用
        UsersApi usersApi = new ParatranzApiFactory().create(UsersApi.class);
        Response<UsersDto> response = usersApi.my().execute();
        Assertions.assertTrue(response.isSuccessful());
        Assertions.assertNotNull(response.body());
        log.info("my info: {}", response.body());
    }

    @Test
    void testGetUser() throws Exception {
        UsersApi usersApi = new ParatranzApiFactory().create(UsersApi.class);
        Response<UsersDto> response = usersApi.getUser(1).execute();
        Assertions.assertTrue(response.isSuccessful());
        Assertions.assertNotNull(response.body());
        log.info("user: {}", response.body());
    }
}
