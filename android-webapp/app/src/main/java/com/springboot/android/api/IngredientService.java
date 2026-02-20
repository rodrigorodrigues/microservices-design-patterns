package com.springboot.android.api;

import com.springboot.android.model.Ingredient;
import com.springboot.android.model.PageResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface IngredientService {

    @GET("ingredient")
    Call<PageResponse<Ingredient>> getIngredients(@Query("page") int page, @Query("size") int size);

    @GET("ingredient/{id}")
    Call<Ingredient> getIngredient(@Path("id") String id);

    @POST("ingredient")
    Call<Ingredient> createIngredient(@Body Ingredient ingredient);

    @PUT("ingredient/{id}")
    Call<Ingredient> updateIngredient(@Path("id") String id, @Body Ingredient ingredient);

    @DELETE("ingredient/{id}")
    Call<Void> deleteIngredient(@Path("id") String id);
}
