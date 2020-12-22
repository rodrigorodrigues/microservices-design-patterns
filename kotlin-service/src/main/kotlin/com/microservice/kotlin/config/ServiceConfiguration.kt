package com.microservice.kotlin.config

import com.microservice.authentication.autoconfigure.AuthenticationProperties
import com.microservice.authentication.common.service.Base64DecodeUtil
import com.microservice.kotlin.repository.TaskRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.core.io.FileSystemResource
import org.springframework.data.mongodb.config.EnableMongoAuditing
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.security.rsa.crypto.KeyStoreKeyFactory
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import java.security.interfaces.RSAPublicKey

@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackageClasses = [TaskRepository::class])
class ServiceConfiguration {
    @Profile("kubernetes")
    @ConditionalOnMissingBean
    @Bean
    fun keyPair(properties: AuthenticationProperties): RSAPublicKey? {
        val jwt = properties.jwt
        val password = Base64DecodeUtil.decodePassword(jwt.keyStorePassword)
        val keyStoreKeyFactory = KeyStoreKeyFactory(FileSystemResource(jwt.keyStore.replaceFirst("file:".toRegex(), "")), password)
        return keyStoreKeyFactory.getKeyPair(jwt.keyAlias).public as RSAPublicKey
    }

    @Bean
    fun validatingMongoEventListener(validator: LocalValidatorFactoryBean): ValidatingMongoEventListener = ValidatingMongoEventListener(validator)

    @Primary
    @Bean
    fun validator(): LocalValidatorFactoryBean = LocalValidatorFactoryBean()
}
