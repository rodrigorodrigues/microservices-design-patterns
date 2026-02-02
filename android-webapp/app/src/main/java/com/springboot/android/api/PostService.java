package com.springboot.android.api;

import com.springboot.android.model.PageResponse;
import com.springboot.android.model.Post;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface PostService {
    @GET("api/posts")
    Call<PageResponse<Post>> getPosts(@Query("page") int page, @Query("size") int size);

    @GET("api/posts/{id}")
    Call<Post> getPost(@Path("id") Long id);

    @POST("api/posts")
    Call<Post> createPost(@Body Post post);

    @PUT("api/posts/{id}")
    Call<Post> updatePost(@Path("id") Long id, @Body Post post);

    @DELETE("api/posts/{id}")
    Call<Void> deletePost(@Path("id") Long id);
}
