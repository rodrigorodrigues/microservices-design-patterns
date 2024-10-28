package com.microservice.web.autoconfigure;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static java.util.Map.entry;

@Data
@ConfigurationProperties(prefix = "com.microservice.web")
public class WebConfigurationProperties implements InitializingBean {
    @NotBlank
    private String kafkaTopic = "topic2";

    @NotEmpty
    private Map<String, String> tenants = new HashMap<>();

    @Override
    public void afterPropertiesSet() {
        if (tenants.isEmpty()) {
            tenants.putAll(Map.ofEntries(
                entry("https://spendingbetter.com", "https://spendingbetter.com/.well-known/jwks.json")
            ));
        }
    }
}
