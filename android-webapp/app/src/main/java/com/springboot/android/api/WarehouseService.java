package com.springboot.android.api;

import com.springboot.android.model.PageResponse;
import com.springboot.android.model.Warehouse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface WarehouseService {

    @GET("api/warehouses")
    Call<PageResponse<Warehouse>> getWarehouses(
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("api/warehouses/{id}")
    Call<Warehouse> getWarehouse(@Path("id") String id);

    @POST("api/warehouses")
    Call<Warehouse> createWarehouse(@Body Warehouse warehouse);

    @PUT("api/warehouses/{id}")
    Call<Warehouse> updateWarehouse(@Path("id") String id, @Body Warehouse warehouse);

    @DELETE("api/warehouses/{id}")
    Call<Void> deleteWarehouse(@Path("id") String id);
}
