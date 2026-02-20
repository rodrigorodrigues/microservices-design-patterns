package com.springboot.android.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Permission {
    @SerializedName("type")
    private String type;

    @SerializedName("permissions")
    private List<String> permissions;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}
