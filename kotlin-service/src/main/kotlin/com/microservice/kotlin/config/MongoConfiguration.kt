package com.microservice.kotlin.config

import com.microservice.kotlin.repository.TaskRepository
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.config.EnableMongoAuditing
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackageClasses = [TaskRepository::class])
class MongoConfiguration()
