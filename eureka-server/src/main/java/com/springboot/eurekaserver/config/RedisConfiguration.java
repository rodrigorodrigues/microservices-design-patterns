package com.springboot.eurekaserver.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.context.AbstractHttpSessionApplicationInitializer;

@Slf4j
@ConditionalOnProperty(prefix = "configuration", name = "redis", havingValue = "true", matchIfMissing = true)
@Configuration
@EnableRedisHttpSession
public class RedisConfiguration extends AbstractHttpSessionApplicationInitializer {
    public RedisConfiguration() {
        log.debug("Set Redis Configuration: {}", this);
    }

    @Bean
    public LettuceConnectionFactory connectionFactory(RedisProperties redisProperties) {
        return new LettuceConnectionFactory(redisProperties.getHost(), redisProperties.getPort());
    }
}
