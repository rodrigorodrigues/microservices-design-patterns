package com.springboot.edgeserver;

import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import com.springboot.edgeserver.config.AuthenticationZuulFilter;
import com.springboot.edgeserver.util.ReactiveSharedAuthenticationServiceImpl;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.oauth2.provider.approval.TokenApprovalStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.reactive.config.EnableWebFlux;

@Slf4j
@SpringBootApplication
@EnableWebFlux
@EnableRedisHttpSession
@EnableDiscoveryClient
public class EdgeServerApplication {

	@Autowired
	AuthenticationZuulFilter authenticationZuulFilter;

	public static void main(String[] args) {
		SpringApplication.run(EdgeServerApplication.class, args);
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

	@Bean
	ReactiveUserDetailsService reactiveUserDetailsService(AuthenticationCommonRepository authenticationCommonRepository) {
		return new ReactiveSharedAuthenticationServiceImpl(authenticationCommonRepository);
	}

	@Bean
	public CookieSerializer cookieSerializer() {
		DefaultCookieSerializer serializer = new DefaultCookieSerializer();
		serializer.setCookieName("SESSIONID");
		serializer.setCookiePath("/");
		return serializer;
	}

	@Bean
	public RouteLocator apiRoutes(RouteLocatorBuilder builder, @Value("${grafanaUrl:http://localhost:3000}") String grafanaUrl) {
		return builder.routes()
				.route("authenticationZuulFilter",p -> p
						.path("/grafana/**")
						.filters(f -> f.filter(authenticationZuulFilter))
						.uri(grafanaUrl))
				.build();
	}

}

