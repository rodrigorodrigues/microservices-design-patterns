package com.springboot.android.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Recipe {
    @SerializedName("_id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("categories")
    private List<Category> categories;

    @SerializedName("weekDay")
    private String weekDay;

    @SerializedName("isInMenuWeek")
    private boolean isInMenuWeek;

    @SerializedName("mainMealValue")
    private String mainMealValue;

    @SerializedName("updateDate")
    private String updateDate;

    @SerializedName("insertDate")
    private String insertDate;

    public Recipe() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<Category> getCategories() { return categories; }
    public void setCategories(List<Category> categories) { this.categories = categories; }

    public String getWeekDay() { return weekDay; }
    public void setWeekDay(String weekDay) { this.weekDay = weekDay; }

    public boolean isInMenuWeek() { return isInMenuWeek; }
    public void setInMenuWeek(boolean inMenuWeek) { isInMenuWeek = inMenuWeek; }

    public String getMainMealValue() { return mainMealValue; }
    public void setMainMealValue(String mainMealValue) { this.mainMealValue = mainMealValue; }

    public String getUpdateDate() { return updateDate; }
    public void setUpdateDate(String updateDate) { this.updateDate = updateDate; }

    public String getInsertDate() { return insertDate; }
    public void setInsertDate(String insertDate) { this.insertDate = insertDate; }
}
