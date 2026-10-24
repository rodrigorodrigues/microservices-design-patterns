package com.springboot.android.api;

import com.google.gson.JsonObject;
import com.springboot.android.model.Passkey;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface PasskeyService {

    @GET("api/webauthns")
    Call<List<Passkey>> getPasskeys();

    @DELETE("api/webauthns/{id}")
    Call<Void> deletePasskey(@Path("id") String id);

    @POST("webauthn/register/options")
    Call<JsonObject> getRegisterOptions();

    @POST("webauthn/register")
    Call<Void> register(@Body JsonObject registrationPayload);

    @POST("webauthn/authenticate/options")
    Call<JsonObject> getAuthenticateOptions();

    @POST("login/webauthn")
    Call<Void> loginWithPasskey(@Body JsonObject assertionPayload);
}
