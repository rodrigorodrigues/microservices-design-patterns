package com.microservice.person.config;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "com.microservice.person")
@Data
public class ConfigProperties {
    @NotBlank
    private String postApi = "http://go-service/api/posts";
    @NotBlank
    private String userApi = "http://user-service/api/posts";
}
