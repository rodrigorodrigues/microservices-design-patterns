package com.microservice.jwt.autoconfigure;

import com.microservice.jwt.common.TokenProvider;
import com.microservice.jwt.common.config.Java8SpringConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(Java8SpringConfigurationProperties.class)
public class JwtCommonAutoConfiguration {
    @Bean
    TokenProvider tokenProvider(Java8SpringConfigurationProperties java8SpringConfigurationProperties) {
        return new TokenProvider(java8SpringConfigurationProperties);
    }
}
