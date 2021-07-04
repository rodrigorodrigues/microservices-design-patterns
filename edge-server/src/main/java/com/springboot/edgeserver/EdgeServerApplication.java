package com.springboot.edgeserver;

import java.util.Arrays;

import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.builder.ToStringBuilder;
import ru.reliabletech.zuul.swagger.EnableZuulSpringfoxSwagger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Slf4j
@SpringBootApplication
@EnableZuulProxy
@EnableZuulSpringfoxSwagger
@EnableRedisHttpSession
public class EdgeServerApplication {
	@Autowired
	DiscoveryClient discoveryClient;

	public static void main(String[] args) {
		SpringApplication.run(EdgeServerApplication.class, args);
	}

	@PostConstruct
	public void test() {
		discoveryClient.getServices().stream()
				.flatMap(s -> discoveryClient.getInstances(s).stream())
				.forEach(s -> log.debug("instance: {}", ToStringBuilder.reflectionToString(s)));
	}

	@Bean
	RedisConnectionFactory lettuceConnectionFactory(RedisProperties redisProperties) {
		return new LettuceConnectionFactory(redisProperties.getHost(), redisProperties.getPort());
	}

	@Bean
	public CookieSerializer cookieSerializer() {
		DefaultCookieSerializer serializer = new DefaultCookieSerializer();
		serializer.setCookieName("SESSIONID");
		serializer.setCookiePath("/");
		return serializer;
	}

	@Bean
	CorsFilter corsWebFilter(Environment environment) {
		log.debug("active profiles: {}", environment.getActiveProfiles());
		CorsConfiguration corsConfig = new CorsConfiguration();
		corsConfig.setAllowCredentials(true);
		if (environment.acceptsProfiles(Profiles.of("prod"))) {
			corsConfig.addAllowedOrigin("https://spendingbetter.com");
		} else {
			corsConfig.addAllowedOrigin("*");
		}
		corsConfig.addAllowedHeader("*");
		corsConfig.addAllowedMethod("*");

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", corsConfig);

		return new CorsFilter(source);
	}

}

