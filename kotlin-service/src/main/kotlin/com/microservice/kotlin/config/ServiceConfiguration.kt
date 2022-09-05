package com.microservice.kotlin.config

import com.github.javafaker.Faker
import com.microservice.kotlin.model.Task
import com.microservice.kotlin.repository.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.mongodb.config.EnableMongoAuditing
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import java.util.*
import java.util.stream.IntStream

@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackageClasses = [TaskRepository::class])
class ServiceConfiguration {
    private val log = LoggerFactory.getLogger(javaClass)

    var faker: Faker = Faker()

    @Bean
    fun validatingMongoEventListener(validator: LocalValidatorFactoryBean): ValidatingMongoEventListener = ValidatingMongoEventListener(validator)

    @Primary
    @Bean
    fun validator(): LocalValidatorFactoryBean = LocalValidatorFactoryBean()

    @Bean
    @ConditionalOnMissingBean
    fun buildProperties(): BuildProperties = BuildProperties(Properties())

    @ConditionalOnProperty(prefix = "load.data", name = ["tasks"], havingValue = "true")
    @Bean
    fun runner(
        @Value("\${load.data.tasks.total:20}") total: Int?,
        taskRepository: TaskRepository
    ): CommandLineRunner? {
        return CommandLineRunner {
            if (taskRepository.count() == 0L) {
                val book = faker.book()
                IntStream.range(0, total!!).forEach {
                    val task = Task(name = book.genre())
                    log.info("task: {}", taskRepository.save(task))
                }
            }
        }
    }
}
