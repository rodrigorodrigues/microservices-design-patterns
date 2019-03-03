package com.springboot.eurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@EnableRedisHttpSession
@EnableEurekaServer
public class EurekaServerApplication implements WebMvcConfigurer {

	public static void main(String[] args) {
		SpringApplication.run(EurekaServerApplication.class, args);
	}

    @Bean
    public LettuceConnectionFactory connectionFactory(RedisProperties redisProperties) {
        return new LettuceConnectionFactory(redisProperties.getHost(), redisProperties.getPort());
    }

}

