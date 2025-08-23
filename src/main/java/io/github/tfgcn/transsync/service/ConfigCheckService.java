package io.github.tfgcn.transsync.service;

import io.github.tfgcn.transsync.paratranz.ParatranzApiFactory;
import io.github.tfgcn.transsync.paratranz.api.UsersApi;
import io.github.tfgcn.transsync.paratranz.model.users.UsersDto;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Response;

import java.io.IOException;

@Slf4j
public class ConfigCheckService {

    public boolean checkGithubConnected() {
        return false;
    }

    public boolean checkParatranzConnected() throws IOException {
        ParatranzApiFactory factory;
        try {
            factory = new ParatranzApiFactory();
        } catch (IOException e) {
            return false;
        }

        UsersApi usersApi = factory.create(UsersApi.class);

        try {
            Response<UsersDto> response = usersApi.my().execute();
            if (response.isSuccessful()) {
                log.info("Paratranz 登录成功: {}", response.body());
                return true;
            }
        } catch (Exception e) {
            log.error("Paratranz 登录失败", e);
            throw e;
        }
        return false;
    }
}