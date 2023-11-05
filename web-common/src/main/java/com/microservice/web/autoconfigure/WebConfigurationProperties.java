package com.microservice.web.autoconfigure;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "com.microservice.web")
public class WebConfigurationProperties {
    @NotBlank
    private String kafkaTopic = "topic2";
}
