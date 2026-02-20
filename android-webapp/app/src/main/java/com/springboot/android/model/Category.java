package com.springboot.android.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Category {
    @SerializedName("_id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("products")
    private List<CategoryProduct> products;

    @SerializedName("updateDate")
    private String updateDate;

    @SerializedName("insertDate")
    private String insertDate;

    public Category() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<CategoryProduct> getProducts() { return products; }
    public void setProducts(List<CategoryProduct> products) { this.products = products; }

    public String getUpdateDate() { return updateDate; }
    public void setUpdateDate(String updateDate) { this.updateDate = updateDate; }

    public String getInsertDate() { return insertDate; }
    public void setInsertDate(String insertDate) { this.insertDate = insertDate; }
}
