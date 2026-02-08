package com.springboot.android.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Person {
    private String id;

    @SerializedName("fullName")
    private String fullName;

    @SerializedName("dateOfBirth")
    private String dateOfBirth;

    @SerializedName("children")
    private List<Children> children;

    @SerializedName("address")
    private Address address;

    @SerializedName("posts")
    private List<Post> posts;

    @SerializedName("user")
    private User user;

    // Additional fields from backend
    @SerializedName("createdByUser")
    private String createdByUser;

    @SerializedName("createdDate")
    private String createdDate;

    @SerializedName("lastModifiedByUser")
    private String lastModifiedByUser;

    @SerializedName("lastModifiedDate")
    private String lastModifiedDate;

    // Inner classes matching backend PersonDto structure
    public static class Children {
        private String name;

        @SerializedName("dateOfBirth")
        private String dateOfBirth;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDateOfBirth() { return dateOfBirth; }
        public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    }

    public static class Address {
        private String id;
        private String address;
        private String city;

        @SerializedName("stateOrProvince")
        private String stateOrProvince;

        private String country;

        @SerializedName("postalCode")
        private String postalCode;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getStateOrProvince() { return stateOrProvince; }
        public void setStateOrProvince(String stateOrProvince) { this.stateOrProvince = stateOrProvince; }
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
        public String getPostalCode() { return postalCode; }
        public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    }

    public static class Post {
        private String id;
        private String name;

        @SerializedName("createdByUser")
        private String createdByUser;

        @SerializedName("createdDate")
        private String createdDate;

        @SerializedName("lastModifiedByUser")
        private String lastModifiedByUser;

        @SerializedName("lastModifiedDate")
        private String lastModifiedDate;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCreatedByUser() { return createdByUser; }
        public void setCreatedByUser(String createdByUser) { this.createdByUser = createdByUser; }
        public String getCreatedDate() { return createdDate; }
        public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }
        public String getLastModifiedByUser() { return lastModifiedByUser; }
        public void setLastModifiedByUser(String lastModifiedByUser) { this.lastModifiedByUser = lastModifiedByUser; }
        public String getLastModifiedDate() { return lastModifiedDate; }
        public void setLastModifiedDate(String lastModifiedDate) { this.lastModifiedDate = lastModifiedDate; }
    }

    public static class User {
        private String status;
        private String name;

        @SerializedName("requestId")
        private String requestId;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public List<Children> getChildren() {
        return children;
    }

    public void setChildren(List<Children> children) {
        this.children = children;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public List<Post> getPosts() {
        return posts;
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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
