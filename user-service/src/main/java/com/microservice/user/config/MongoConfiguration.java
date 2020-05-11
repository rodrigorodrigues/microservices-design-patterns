package com.microservice.user.config;

import com.microservice.user.repository.UserRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@Configuration
@EnableMongoAuditing
@EnableReactiveMongoRepositories(basePackageClasses = UserRepository.class)
public class MongoConfiguration {
}
