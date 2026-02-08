package com.springboot.android.api;

import com.springboot.android.model.PageResponse;
import com.springboot.android.model.Product;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ProductService {

    @GET("api/products")
    Call<PageResponse<Product>> getProducts(
            @Query("page") int page,
            @Query("size") int size,
            @Query("search") String search
    );

    @GET("api/products/{id}")
    Call<Product> getProduct(@Path("id") String id);

    @POST("api/products")
    Call<Product> createProduct(@Body Product product);

    @PUT("api/products/{id}")
    Call<Product> updateProduct(@Path("id") String id, @Body Product product);

    @DELETE("api/products/{id}")
    Call<Void> deleteProduct(@Path("id") String id);

    @GET("api/products/search")
    Call<List<Product>> searchProducts(@Query("name") String name);
}
