package com.springboot.android.api;

import com.springboot.android.model.Company;
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

public interface CompanyService {

    @GET("api/companies")
    Call<PageResponse<Company>> getCompanies(
            @Query("page") int page,
            @Query("size") int size,
            @Query("search") String search
    );

    @GET("api/companies/{id}")
    Call<Company> getCompany(@Path("id") String id);

    @POST("api/companies")
    Call<Company> createCompany(@Body Company company);

    @PUT("api/companies/{id}")
    Call<Company> updateCompany(@Path("id") String id, @Body Company company);

    @DELETE("api/companies/{id}")
    Call<Void> deleteCompany(@Path("id") String id);

    @GET("api/companies/search")
    Call<List<Company>> searchCompanies(@Query("name") String name);
}
