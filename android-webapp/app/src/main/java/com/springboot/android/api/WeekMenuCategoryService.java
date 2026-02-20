package com.springboot.android.api;

import com.springboot.android.model.Category;
import com.springboot.android.model.PageResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface WeekMenuCategoryService {

    @GET("api/v2/category")
    Call<PageResponse<Category>> getCategories(
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("api/v2/category/{id}")
    Call<Category> getCategory(@Path("id") String id);

    @POST("api/v2/category")
    Call<Category> createCategory(@Body Category category);

    @PUT("api/v2/category/{id}")
    Call<Category> updateCategory(@Path("id") String id, @Body Category category);

    @DELETE("api/v2/category/{id}")
    Call<Void> deleteCategory(@Path("id") String id);
}
