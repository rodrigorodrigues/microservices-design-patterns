package com.microservice.person.config;

import com.microservice.person.repository.PersonRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackageClasses = PersonRepository.class)
public class MongoConfiguration {
}
