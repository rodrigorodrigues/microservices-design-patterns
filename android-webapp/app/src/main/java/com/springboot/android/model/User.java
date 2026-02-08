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

    private String password;

    @SerializedName("currentPassword")
    private String currentPassword;

    @SerializedName("confirmPassword")
    private String confirmPassword;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }
}
