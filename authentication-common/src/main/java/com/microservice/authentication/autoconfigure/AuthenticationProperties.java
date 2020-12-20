package com.microservice.authentication.autoconfigure;

import lombok.Data;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "com.microservice.authentication")
public class AuthenticationProperties extends ResourceServerProperties {
    private String issuer = "https://spendingbetter.com";
    private String aud = "https://spendingbetter.com";
}
