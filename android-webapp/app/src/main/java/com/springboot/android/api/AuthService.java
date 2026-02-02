package com.springboot.android.api;

import com.springboot.android.model.AccountInfo;
import com.springboot.android.model.CsrfToken;
import com.springboot.android.model.LoginRequest;
import com.springboot.android.model.LoginResponse;
import com.springboot.android.model.User;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface AuthService {

    @GET("api/csrf")
    Call<CsrfToken> getCsrfToken();

    @FormUrlEncoded
    @POST("api/authenticate")
    Call<LoginResponse> login(@Field("username") String username, @Field("password") String password);

    @GET("api/account")
    Call<AccountInfo> getAccount();

    @GET("api/authenticate")
    Call<User> getCurrentUser();

    @POST("api/logout")
    Call<Void> logout();
}
