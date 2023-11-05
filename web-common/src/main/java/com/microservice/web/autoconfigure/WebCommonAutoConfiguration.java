package com.microservice.web.autoconfigure;

import com.microservice.web.common.util.CustomDefaultErrorAttributes;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableConfigurationProperties(WebConfigurationProperties.class)
public class WebCommonAutoConfiguration {
    @Primary
    @Bean
    CustomDefaultErrorAttributes customDefaultErrorAttributes() {
        return new CustomDefaultErrorAttributes();
    }
}
