package com.springboot.android.api;

import com.springboot.android.model.PageResponse;
import com.springboot.android.model.Person;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface PersonService {

    @GET("api/people")
    Call<PageResponse<Person>> getPersons(
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("api/people/{id}")
    Call<Person> getPerson(@Path("id") String id);

    @POST("api/people")
    Call<Person> createPerson(@Body Person person);

    @PUT("api/people/{id}")
    Call<Person> updatePerson(@Path("id") String id, @Body Person person);

    @DELETE("api/people/{id}")
    Call<Void> deletePerson(@Path("id") String id);

    @GET("api/people/search")
    Call<List<Person>> searchPersons(@Query("name") String name);
}
