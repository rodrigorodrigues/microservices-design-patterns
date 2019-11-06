package com.microservice.authentication.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "configuration")
public class AuthenticationProperties {
    private List<String> authorizeUrls = new ArrayList<>();
}
