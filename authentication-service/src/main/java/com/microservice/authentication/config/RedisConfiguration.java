package com.microservice.authentication.config;

import com.microservice.authentication.service.CustomLogoutSuccessHandler;
import com.microservice.authentication.service.RedisTokenStoreService;
import com.microservice.authentication.service.RedisTokenStoreServiceImpl;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.security.oauth2.provider.approval.TokenApprovalStore;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@ConditionalOnProperty(value = "com.microservice.authentication.redis.enabled", havingValue = "true", matchIfMissing = true)
@Configuration
@Slf4j
public class RedisConfiguration {
    public RedisConfiguration() {
        log.info("RedisConfiguration:constructor");
    }

    @Configuration
    @EnableRedisHttpSession
    class HttpRedisConfiguration {
        HttpRedisConfiguration() {
            log.info("HttpRedisConfiguration:constructor");
        }
    }

    @ConditionalOnMissingBean
    @Bean
    RedisConnectionFactory lettuceConnectionFactory(RedisProperties redisProperties) {
        return new LettuceConnectionFactory(redisProperties.getHost(), redisProperties.getPort());
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

    @Primary
    @Bean
    RedisTokenStoreService redisTokenStoreService(DefaultTokenServices defaultTokenServices, RedisTokenStore redisTokenStore) {
        return new RedisTokenStoreServiceImpl(defaultTokenServices, redisTokenStore);
    }

    @Primary
    @Bean
    LogoutSuccessHandler customLogoutSuccessHandler(RedisTokenStoreServiceImpl redisTokenStoreService) {
        return new CustomLogoutSuccessHandler(redisTokenStoreService);
    }
}
