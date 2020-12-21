package com.microservice.kotlin.config

import com.microservice.authentication.autoconfigure.AuthenticationProperties
import com.microservice.kotlin.repository.TaskRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.web.ServerProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.data.mongodb.config.EnableMongoAuditing
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.security.rsa.crypto.KeyStoreKeyFactory
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter
import java.security.interfaces.RSAPublicKey
import java.util.*

@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackageClasses = [TaskRepository::class])
class ServiceConfiguration(@Autowired val environment: Environment) {
    @Profile("kubernetes & !test")
    @ConditionalOnMissingBean
    @Bean
    fun keyPair(properties: AuthenticationProperties): RSAPublicKey? {
        val jwt = properties.jwt
        val password = getSslPassword(jwt.keyStorePassword)
        val keyStoreKeyFactory = KeyStoreKeyFactory(FileSystemResource(jwt.keyStore.replaceFirst("file:".toRegex(), "")), password)
        return keyStoreKeyFactory.getKeyPair(jwt.keyAlias).public as RSAPublicKey
    }

    @Profile("kubernetes__")
    @ConditionalOnMissingBean
    @Bean
    fun keyPairSsl(@Value("\${server.ssl.key-store}") keystore: Resource?, serverProperties: ServerProperties): RSAPublicKey? {
        val ssl = serverProperties.ssl
        return KeyStoreKeyFactory(keystore, ssl.keyStorePassword.toCharArray())
            .getKeyPair(ssl.keyAlias)
            .public as RSAPublicKey
    }

    private fun getSslPassword(password: String): CharArray? {
        return try {
            String(Base64.getDecoder().decode(password)).toCharArray()
        } catch (e: Exception) {
            password.toCharArray()
        }
    }

    @Bean
    fun validatingMongoEventListener(validator: LocalValidatorFactoryBean): ValidatingMongoEventListener = ValidatingMongoEventListener(validator)

    @Primary
    @Bean
    fun validator(): LocalValidatorFactoryBean = LocalValidatorFactoryBean()

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
