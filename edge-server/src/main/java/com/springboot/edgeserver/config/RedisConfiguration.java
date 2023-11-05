package com.springboot.edgeserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.jackson2.SecurityJackson2Modules;
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
public class RedisConfiguration implements BeanClassLoaderAware {
    private ClassLoader loader;

    public RedisConfiguration() {
        log.info("RedisConfiguration:constructor");
    }

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new GenericJackson2JsonRedisSerializer(objectMapper());
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

    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules(SecurityJackson2Modules.getModules(this.loader));
        mapper.registerModule(new OAuth2AuthenticationJackson2Module());
        mapper.registerModule(new OAuth2RequestModule());
        return mapper;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.loader = classLoader;
    }
}
