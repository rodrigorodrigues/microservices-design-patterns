package com.microservice.kotlin.config

import com.microservice.kotlin.model.Task
import com.microservice.kotlin.repository.TaskRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.data.mongodb.config.EnableMongoAuditing
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackageClasses = [TaskRepository::class])
class ServiceConfiguration(@Autowired val environment: Environment) {

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

    @Bean
    fun corsWebFilter(): CorsFilter {
        val corsConfig = CorsConfiguration()
        corsConfig.addAllowedHeader("*")
        corsConfig.addAllowedMethod("*")
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            corsConfig.addAllowedOrigin("https://spendingbetter.com")
        } else {
            corsConfig.addAllowedOrigin("*")
        }
        corsConfig.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", corsConfig)

        return CorsFilter(source)
    }
}
