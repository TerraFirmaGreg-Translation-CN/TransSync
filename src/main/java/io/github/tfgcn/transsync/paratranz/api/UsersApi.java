package io.github.tfgcn.transsync.paratranz.api;

import io.github.tfgcn.transsync.paratranz.model.users.UsersDto;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface UsersApi {

    @GET("users/{userId}")
    Call<UsersDto> getUser(@Path("userId") Integer userId);

    @GET("users/my")
    Call<UsersDto> my();
}
