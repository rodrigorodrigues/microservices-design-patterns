package com.springboot.edgeserver.config;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.provider.approval.TokenApprovalStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.session.data.redis.config.annotation.web.server.EnableRedisWebSession;

@ConditionalOnProperty(value = "com.microservice.authentication.redis.enabled", havingValue = "true")
@Configuration
@EnableRedisWebSession
@Slf4j
public class RedisConfiguration {
    public RedisConfiguration() {
        log.info("RedisConfiguration:constructor");
    }

    @Primary
    @Bean
    RedisTokenStore redisTokenStore(RedisConnectionFactory redisConnectionFactory,
            JwtAccessTokenConverter jwtAccessTokenConverter) {
        RedisTokenStore redisTokenStore = new RedisTokenStore(redisConnectionFactory);
        TokenApprovalStore tokenApprovalStore = new TokenApprovalStore();
        tokenApprovalStore.setTokenStore(redisTokenStore);
        JwtTokenStore jwtTokenStore = new JwtTokenStore(jwtAccessTokenConverter);
        jwtTokenStore.setApprovalStore(tokenApprovalStore);
        return redisTokenStore;
    }

    @Bean
    RedisIndexedSessionRepository redisIndexedSessionRepository(RedisTemplate redisTemplate) {
        return new RedisIndexedSessionRepository(redisTemplate);
    }
}
