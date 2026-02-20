package com.springboot.android.model;

import com.google.gson.annotations.SerializedName;

public class Ingredient {
    @SerializedName("_id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("categoryName")
    private String categoryName;

    @SerializedName("expiryDate")
    private String expiryDate;

    @SerializedName("updateDate")
    private String updateDate;

    @SerializedName("insertDate")
    private String insertDate;

    public Ingredient() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public String getUpdateDate() { return updateDate; }
    public void setUpdateDate(String updateDate) { this.updateDate = updateDate; }

    public String getInsertDate() { return insertDate; }
    public void setInsertDate(String insertDate) { this.insertDate = insertDate; }
}
