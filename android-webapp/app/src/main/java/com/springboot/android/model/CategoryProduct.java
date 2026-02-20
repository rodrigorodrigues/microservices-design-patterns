package com.springboot.android.model;

import com.google.gson.annotations.SerializedName;

public class CategoryProduct {
    @SerializedName("name")
    private String name;

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("completed")
    private boolean completed;

    @SerializedName("insertDate")
    private String insertDate;

    public CategoryProduct() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public String getInsertDate() { return insertDate; }
    public void setInsertDate(String insertDate) { this.insertDate = insertDate; }
}
