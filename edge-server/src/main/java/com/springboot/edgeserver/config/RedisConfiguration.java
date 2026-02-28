package com.springboot.edgeserver.config;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.server.EnableRedisWebSession;

@ConditionalOnProperty(value = "com.microservice.authentication.redis.enabled", havingValue = "true")
@Configuration
@EnableRedisWebSession
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
}
