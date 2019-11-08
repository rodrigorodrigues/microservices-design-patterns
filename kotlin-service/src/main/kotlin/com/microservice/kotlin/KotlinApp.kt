package com.microservice.kotlin

import com.microservice.kotlin.model.Task
import com.microservice.kotlin.repository.TaskRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import java.util.*

@SpringBootApplication
@EnableDiscoveryClient
class KotlinApp {

    fun main(args: Array<String>) {
        runApplication<KotlinApp>(*args)
    }

    @ConditionalOnProperty(prefix = "configuration", name = ["initialLoad"], havingValue = "true", matchIfMissing = true)
    @Bean
    fun loadInitialData(taskRepository: TaskRepository) : CommandLineRunner {
        return CommandLineRunner {
            if (taskRepository.count() == 0L) {
                val listOf = arrayListOf<Task>()
                var task = Task(UUID.randomUUID().toString(), "Learn new technologies")
                listOf.add(task)

                task = Task(UUID.randomUUID().toString(), "Travel around the world")
                listOf.add(task)

                task = Task(UUID.randomUUID().toString(), "Fix Laptop")
                listOf.add(task)

                taskRepository.saveAll(listOf)
            }
        }
    }

    @Bean
    fun validatingMongoEventListener(validator: LocalValidatorFactoryBean): ValidatingMongoEventListener = ValidatingMongoEventListener(validator)

    @Primary
    @Bean
    fun validator(): LocalValidatorFactoryBean = LocalValidatorFactoryBean()

    @Bean
    fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()


}
