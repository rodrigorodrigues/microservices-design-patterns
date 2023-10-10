package com.microservice.authentication.config;

import com.microservice.authentication.service.CustomLogoutSuccessHandler;
import com.microservice.authentication.service.Oauth2TokenStoreService;
import com.microservice.authentication.service.RedisOauth2TokenStoreServiceImpl;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.provider.approval.TokenApprovalStore;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@ConditionalOnProperty(value = "com.microservice.authentication.redis.enabled", havingValue = "true")
@Configuration
@EnableRedisHttpSession
@Slf4j
public class RedisConfiguration {
    public RedisConfiguration() {
        log.info("RedisConfiguration:constructor");
    }

    @Primary
    @Bean
    RedisIndexedSessionRepository redisIndexedSessionRepository(RedisTemplate redisTemplate) {
        return new RedisIndexedSessionRepository(redisTemplate);
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
    Oauth2TokenStoreService redisTokenStoreService(DefaultTokenServices defaultTokenServices, RedisTokenStore redisTokenStore) {
        return new RedisOauth2TokenStoreServiceImpl(defaultTokenServices, redisTokenStore);
    }

    @Primary
    @Bean
    LogoutSuccessHandler customLogoutSuccessHandler(RedisOauth2TokenStoreServiceImpl redisTokenStoreService) {
        return new CustomLogoutSuccessHandler(redisTokenStoreService);
    }
}
