package com.microservice.authentication.autoconfigure;

import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import com.microservice.authentication.common.service.SharedAuthenticationServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@ConditionalOnProperty(prefix = "configuration", name = "mongo", havingValue = "true", matchIfMissing = true)
@Configuration
@EnableMongoRepositories(basePackageClasses = AuthenticationCommonRepository.class)
@Slf4j
public class AuthenticationCommonMongoConfiguration {
    public AuthenticationCommonMongoConfiguration() {
        log.debug("Set Mongo Configuration: {}", this);
    }

    @Bean
    SharedAuthenticationServiceImpl sharedAuthenticationService(AuthenticationCommonRepository authenticationCommonRepository) {
        return new SharedAuthenticationServiceImpl(authenticationCommonRepository);
    }
}
