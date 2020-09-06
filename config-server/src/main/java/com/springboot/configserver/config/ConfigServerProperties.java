package com.springboot.configserver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.constraints.NotBlank;

@Data
@ConfigurationProperties(prefix = "configuration")
public class ConfigServerProperties {
    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
