package com.learning.springboot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class PersonDto {
    private String id;
    private String name;
    private Integer age;
    private String username;
    private String password;
    private String confirmPassword;
    private String email;
    private List<AuthorityDto> authorities;
    private List<ChildrenDto> children;
    private Address address;

    public PersonDto(String name, Integer age, String email, String username, String password, List<AuthorityDto> authorities) {
        setName(name);
        setAge(age);
        setEmail(email);
        setUsername(username);
        setPassword(password);
        setAuthorities(authorities);
    }

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
