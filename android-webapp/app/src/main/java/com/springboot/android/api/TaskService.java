package com.springboot.android.api;

import com.springboot.android.model.PageResponse;
import com.springboot.android.model.Task;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TaskService {
    @GET("api/tasks")
    Call<PageResponse<Task>> getTasks(
            @Query("page") int page,
            @Query("size") int size,
            @Query("search") String search
    );

    @GET("api/tasks/{id}")
    Call<Task> getTask(@Path("id") String id);

    @POST("api/tasks")
    Call<Task> createTask(@Body Task task);

    @PUT("api/tasks/{id}")
    Call<Task> updateTask(@Path("id") String id, @Body Task task);

    @DELETE("api/tasks/{id}")
    Call<Void> deleteTask(@Path("id") String id);
}
