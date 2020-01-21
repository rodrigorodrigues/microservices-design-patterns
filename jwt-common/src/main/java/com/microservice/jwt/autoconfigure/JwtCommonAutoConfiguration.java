package com.microservice.jwt.autoconfigure;

import com.microservice.jwt.common.config.Java8SpringConfigurationProperties;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(Java8SpringConfigurationProperties.class)
@AllArgsConstructor
public class JwtCommonAutoConfiguration {
}
