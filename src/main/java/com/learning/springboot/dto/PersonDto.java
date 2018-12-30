package com.learning.springboot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class PersonDto {
    private String id;
    private String name;
    private Integer age;
    private String login;
    private String password;
    private String email;
    private List<AuthorityDto> authorities;
    private List<ChildrenDto> children;
    private Address address;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChildrenDto {
        private String name;
        private Integer age;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorityDto {
        private String role;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address {
        private String id;
        private String address;
        private String city;
        private String stateOrProvince;
        private String country;
        private String postalCode;
    }
}
