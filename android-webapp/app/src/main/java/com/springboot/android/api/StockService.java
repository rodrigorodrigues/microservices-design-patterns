package com.springboot.android.api;

import com.springboot.android.model.Stock;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface StockService {

    @GET("api/stocks")
    Call<List<Stock>> getStocks(
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("api/stocks/{id}")
    Call<Stock> getStock(@Path("id") String id);

    @POST("api/stocks")
    Call<Stock> createStock(@Body Stock stock);

    @PUT("api/stocks/{id}")
    Call<Stock> updateStock(@Path("id") String id, @Body Stock stock);

    @DELETE("api/stocks/{id}")
    Call<Void> deleteStock(@Path("id") String id);
}
