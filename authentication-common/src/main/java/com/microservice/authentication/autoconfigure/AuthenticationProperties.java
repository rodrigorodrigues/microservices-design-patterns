package com.microservice.authentication.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "configuration")
public class AuthenticationProperties {
    private String issuer = "https://spendingbetter.com";
    private String aud = "https://spendingbetter.com";
}
