package com.springboot.adminserver;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@SpringBootApplication
@EnableAdminServer
public class AdminServerApplication {
    @Slf4j
    @ConditionalOnProperty(value = "com.microservice.authentication.redis.enabled", havingValue = "true")
    @Configuration
    @EnableRedisHttpSession
    class RedisConfiguration {
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

    public static void main(String[] args) {
        SpringApplication.run(AdminServerApplication.class, args);
    }

    @Primary
    @Bean
    RedisIndexedSessionRepository redisIndexedSessionRepository(RedisTemplate redisTemplate) {
        return new RedisIndexedSessionRepository(redisTemplate);
    }

    @Bean
    static BeanFactoryPostProcessor removeErrorSecurityFilter() {
        return (beanFactory) -> {
            try {
                ((DefaultListableBeanFactory) beanFactory).removeBeanDefinition("errorPageSecurityInterceptor");
            } catch (Exception ignored) {}
        };
    }

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSIONID");
        serializer.setCookiePath("/");
        serializer.setUseBase64Encoding(false);
        return serializer;
    }

    @Bean
    CorsFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.applyPermitDefaultValues();
        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsFilter(source);
    }

    @Bean
    @Order(0)
    public RequestContextListener requestContextListener() {
        return new RequestContextListener();
    }
}
