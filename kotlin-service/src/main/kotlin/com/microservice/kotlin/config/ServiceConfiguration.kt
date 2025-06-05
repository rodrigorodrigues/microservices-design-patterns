package com.microservice.kotlin.config

import com.github.javafaker.Faker
import com.microservice.authentication.autoconfigure.AuthenticationProperties
import com.microservice.kotlin.model.Task
import com.microservice.kotlin.repository.TaskRepository
import com.microservice.web.common.util.ChangeQueryStringFilter
import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.crypto.MACSigner
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Predicate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.info.BuildProperties
import org.springframework.boot.info.GitProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.convert.support.DefaultConversionService
import org.springframework.data.mongodb.config.EnableMongoAuditing
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.data.querydsl.binding.QuerydslBindings
import org.springframework.data.querydsl.binding.QuerydslBindingsFactory
import org.springframework.data.querydsl.binding.QuerydslPredicateBuilder
import org.springframework.data.util.TypeInformation
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.util.Assert
import org.springframework.util.MultiValueMap
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import java.nio.charset.StandardCharsets
import java.security.interfaces.RSAPublicKey
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

    @ConditionalOnMissingBean
    @Bean
    fun gitProperties(): GitProperties? {
        return GitProperties(Properties())
    }

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

    @Primary
    @Profile("prod")
    @Bean
    fun publicKeyStore(@Value("\${com.microservice.authentication.jwt.publicKeyStore}") key: RSAPublicKey?): RSAPublicKey? {
        return key
    }

    @ConditionalOnMissingBean
    @Bean
    fun querydslPredicateBuilderCustomizer(querydslBindingsFactory: QuerydslBindingsFactory): QuerydslPredicateBuilder {
        return object : QuerydslPredicateBuilder(
            DefaultConversionService.getSharedInstance(),
            querydslBindingsFactory.entityPathResolver
        ) {
            override fun getPredicate(
                type: TypeInformation<*>?,
                values: MultiValueMap<String?, *>,
                bindings: QuerydslBindings
            ): Predicate {
                Assert.notNull(bindings, "Context must not be null")

                val builder = BooleanBuilder()

                if (values.isEmpty()) {
                    return getPredicate(builder)
                }

                for ((path, value1) in values) {
                    if (isSingleElementCollectionWithEmptyItem(
                            value1
                        )
                    ) {
                        continue
                    }

                    if (!bindings.isPathAvailable(path, type)) {
                        continue
                    }

                    val propertyPath = bindings.getPropertyPath(path, type) ?: continue

                    val value = convertToPropertyPathSpecificType(value1, propertyPath, conversionService)
                    val predicate = invokeBinding(propertyPath, bindings, value, resolver, defaultBinding)

                    predicate.ifPresent { right: Predicate? ->
                        builder.or(
                            right
                        )
                    }
                }

                return getPredicate(builder)
            }
        }
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun changeQueryStringFilter(): ChangeQueryStringFilter<Task> {
        return object : ChangeQueryStringFilter<Task>() {
            override fun getObjectType(): Class<Task> {
                return Task::class.java
            }

            override fun getEntityType(): Class<out Annotation?> {
                return Document::class.java
            }
        }
    }

    @ConditionalOnProperty(prefix = "com.microservice.authentication.jwt", name = ["key-value"])
    @Primary
    @ConditionalOnMissingBean
    @Bean
    @Throws(
        JOSEException::class
    )
    fun jwtDecoder(properties: AuthenticationProperties): JwtDecoder {
        val jwt = properties.jwt
        val secret = jwt.keyValue.toByteArray(StandardCharsets.UTF_8)

        val macSigner = MACSigner(secret)
        return NimbusJwtDecoder.withSecretKey(macSigner.secretKey).build()
    }
}
