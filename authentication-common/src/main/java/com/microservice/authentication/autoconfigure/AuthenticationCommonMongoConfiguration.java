package com.microservice.authentication.autoconfigure;

import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import com.microservice.authentication.common.service.SharedAuthenticationService;
import com.microservice.authentication.common.service.SharedAuthenticationServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackageClasses = AuthenticationCommonRepository.class)
@Slf4j
public class AuthenticationCommonMongoConfiguration {
    public AuthenticationCommonMongoConfiguration() {
        log.debug("Set authentication common configuration: {}", this);
    }

    @Bean
    SharedAuthenticationService sharedAuthenticationService(AuthenticationCommonRepository authenticationCommonRepository) {
        return new SharedAuthenticationServiceImpl(authenticationCommonRepository);
    }
}
