package com.springboot.android.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Post {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    // Tasks array is optional in the response
    @SerializedName("tasks")
    private List<Task> tasks;

    @SerializedName("createdByUser")
    private String createdByUser;

    @SerializedName("createdDate")
    private String createdDate;

    @SerializedName("lastModifiedByUser")
    private String lastModifiedByUser;

    @SerializedName("lastModifiedDate")
    private String lastModifiedDate;

    public Post() {
    }

    public Post(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    public String getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(String createdByUser) {
        this.createdByUser = createdByUser;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getLastModifiedByUser() {
        return lastModifiedByUser;
    }

    public void setLastModifiedByUser(String lastModifiedByUser) {
        this.lastModifiedByUser = lastModifiedByUser;
    }

    public String getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(String lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
}
