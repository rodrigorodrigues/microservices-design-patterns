package com.springboot.edgeserver;

import com.microservice.authentication.resourceserver.config.ActuatorResourceServerConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
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
import ru.reliabletech.zuul.swagger.EnableZuulSpringfoxSwagger;

import javax.net.ssl.HttpsURLConnection;

@Slf4j
@SpringBootApplication
@EnableZuulProxy
@EnableDiscoveryClient
@Import(ActuatorResourceServerConfiguration.class)
@EnableZuulSpringfoxSwagger
@EnableRedisHttpSession
public class EdgeServerApplication {
	static {
		HttpsURLConnection.setDefaultHostnameVerifier(new NoopHostnameVerifier());
	}

	public static void main(String[] args) {
		SpringApplication.run(EdgeServerApplication.class, args);
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

