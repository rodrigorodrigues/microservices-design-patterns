package com.microservice.authentication.config;

import java.time.Duration;

import com.microservice.authentication.service.CustomLogoutSuccessHandler;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.session.FlushMode;
import org.springframework.session.SessionRepository;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.data.redis.RedisSessionRepository;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@ConditionalOnProperty(value = "com.microservice.authentication.redis.enabled", havingValue = "true")
@Configuration(proxyBeanMethods = false)
@EnableRedisHttpSession(flushMode = FlushMode.IMMEDIATE)
@Slf4j
public class RedisConfiguration {
    public RedisConfiguration() {
        log.info("RedisConfiguration:constructor");
    }

    @ConditionalOnMissingBean
    @Bean
    public LettuceConnectionFactory redisConnectionFactory(DataRedisProperties dataRedisProperties) {
        log.info("Creating LettuceConnectionFactory with host: {}, port: {}, password set: {}",
                 dataRedisProperties.getHost(),
                 dataRedisProperties.getPort(),
                 dataRedisProperties.getPassword() != null && !dataRedisProperties.getPassword().isEmpty());

        org.springframework.data.redis.connection.RedisStandaloneConfiguration config =
            new org.springframework.data.redis.connection.RedisStandaloneConfiguration();
        config.setHostName(dataRedisProperties.getHost());
        config.setPort(dataRedisProperties.getPort());

        // Set password if provided
        if (dataRedisProperties.getPassword() != null && !dataRedisProperties.getPassword().isEmpty()) {
            config.setPassword(dataRedisProperties.getPassword());
        }

        // Set username if provided (for Redis 6+ ACL)
        if (dataRedisProperties.getUsername() != null && !dataRedisProperties.getUsername().isEmpty()) {
            config.setUsername(dataRedisProperties.getUsername());
        }

        return new LettuceConnectionFactory(config);
    }

    /**
     * Idle timeout of login sessions. {@code @EnableRedisHttpSession} defaults to 30 minutes and ignores
     * {@code spring.session.timeout}, so after 30 idle minutes /api/authenticatedUser and /api/refreshToken
     * reject the session even though the access token in it is still valid. Defaults to the access token
     * lifetime set in {@link com.microservice.authentication.service.GenerateToken} (10 hours).
     */
    @Bean
    SessionRepositoryCustomizer<RedisSessionRepository> sessionTimeoutCustomizer(
        @Value("${com.microservice.authentication.session-timeout:10h}") Duration sessionTimeout) {
        log.info("Session idle timeout: {}", sessionTimeout);
        return sessionRepository -> sessionRepository.setDefaultMaxInactiveInterval(sessionTimeout);
    }

    @Primary
    @Bean
    LogoutSuccessHandler customLogoutSuccessHandler(SessionRepository sessionRepository) {
        return new CustomLogoutSuccessHandler(sessionRepository);
    }
}
