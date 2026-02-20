package com.springboot.android.api;

import com.springboot.android.model.PageResponse;
import com.springboot.android.model.Recipe;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RecipeService {

    @GET("v2/recipe")
    Call<PageResponse<Recipe>> getRecipes(@Query("page") int page, @Query("size") int size);

    @GET("v2/recipe/{id}")
    Call<Recipe> getRecipe(@Path("id") String id);

    @POST("v2/recipe")
    Call<Recipe> createRecipe(@Body Recipe recipe);

    @PUT("v2/recipe/{id}")
    Call<Recipe> updateRecipe(@Path("id") String id, @Body Recipe recipe);

    @DELETE("v2/recipe/{id}")
    Call<Void> deleteRecipe(@Path("id") String id);
}
