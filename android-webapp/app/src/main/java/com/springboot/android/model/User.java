package com.springboot.android.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String id;
    private String username;
    private String email;

    @SerializedName("fullName")
    private String fullName;

    private boolean activated;

    @SerializedName("authorities")
    private List<Authority> authorities;

    @SerializedName("imageUrl")
    private String imageUrl;

    // Additional fields that might come from backend
    @SerializedName("createdByUser")
    private String createdByUser;

    @SerializedName("createdDate")
    private String createdDate;

    @SerializedName("lastModifiedByUser")
    private String lastModifiedByUser;

    @SerializedName("lastModifiedDate")
    private String lastModifiedDate;

    @SerializedName("userType")
    private String userType;

    // Inner class to match the authorities JSON structure
    public static class Authority {
        @SerializedName("role")
        private String role;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    // Helper method to get authorities as list of strings
    public List<String> getAuthoritiesAsStrings() {
        List<String> roles = new ArrayList<>();
        if (authorities != null) {
            for (Authority auth : authorities) {
                if (auth.getRole() != null) {
                    roles.add(auth.getRole());
                }
            }
        }
        return roles;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public List<Authority> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(List<Authority> authorities) {
        this.authorities = authorities;
    }
}
