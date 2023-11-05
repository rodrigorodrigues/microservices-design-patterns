package com.microservice.person.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PersonDto {
    private String id;
    private String fullName;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;
    private List<ChildrenDto> children;
    private Address address;
    private String createdByUser;
    private Instant createdDate;
    private String lastModifiedByUser;
    private Instant lastModifiedDate;
    private List<Post> posts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChildrenDto {
        private String name;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate dateOfBirth;
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Post {
        private String id;
        private String name;
        private String createdByUser;
        private String createdDate;
        private String lastModifiedByUser;
        private String lastModifiedDate;
    }
}
