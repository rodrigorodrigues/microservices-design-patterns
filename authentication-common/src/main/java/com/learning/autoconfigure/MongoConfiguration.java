package com.learning.autoconfigure;

import com.learning.springboot.repository.AuthenticationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@ConditionalOnProperty(prefix = "configuration", name = "mongo", havingValue = "true", matchIfMissing = true)
@Configuration
@EnableMongoRepositories(basePackageClasses = AuthenticationRepository.class)
@Slf4j
@ComponentScan(basePackages = "com.learning.springboot")
public class MongoConfiguration {
    public MongoConfiguration() {
        log.debug("Set Mongo Configuration: {}", this);
    }
}
