package com.microservice.kotlin.config

import com.microservice.kotlin.model.Task
import com.microservice.kotlin.repository.TaskRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.mongodb.config.EnableMongoAuditing
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean

@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackageClasses = [TaskRepository::class])
class ServiceConfiguration {

    @ConditionalOnProperty(prefix = "configuration", name = ["initialLoad"], havingValue = "true", matchIfMissing = true)
    @Bean
    fun loadInitialData(taskRepository: TaskRepository) : CommandLineRunner {
        return CommandLineRunner {
            if (taskRepository.count() == 0L) {
                val listOf = arrayListOf(
                    Task(name =  "Learn new technologies"),
                    Task(name =  "Travel around the world"),
                    Task(name =  "Fix Laptop")
                )
                taskRepository.saveAll(listOf)
            }
        }
    }

    @Bean
    fun validatingMongoEventListener(validator: LocalValidatorFactoryBean): ValidatingMongoEventListener = ValidatingMongoEventListener(validator)

    @Primary
    @Bean
    fun validator(): LocalValidatorFactoryBean = LocalValidatorFactoryBean()

    @Primary
    @Bean
    fun customDefaultErrorAttributes(): CustomDefaultErrorAttributes = CustomDefaultErrorAttributes()
}
