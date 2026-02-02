package com.springboot.android.api;

import com.springboot.android.model.PageResponse;
import com.springboot.android.model.User;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface UserService {

    @GET("api/users")
    Call<PageResponse<User>> getUsers(
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("api/users/{id}")
    Call<User> getUser(@Path("id") String id);

    @POST("api/users")
    Call<User> createUser(@Body User user);

    @PUT("api/users/{id}")
    Call<User> updateUser(@Path("id") String id, @Body User user);

    @DELETE("api/users/{id}")
    Call<Void> deleteUser(@Path("id") String id);
}
