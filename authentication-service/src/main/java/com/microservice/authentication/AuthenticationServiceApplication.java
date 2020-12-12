package com.microservice.authentication;

import com.microservice.authentication.common.model.Authentication;
import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import com.microservice.authentication.config.AuthenticationProperties;
import com.microservice.authentication.resourceserver.config.ActuatorResourceServerConfiguration;
import com.microservice.authentication.service.CustomLogoutSuccessHandler;
import com.microservice.authentication.service.RedisTokenStoreService;
import com.microservice.authentication.web.util.CustomDefaultErrorAttributes;
import com.microservice.web.common.util.constants.DefaultUsers;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.stereotype.Controller;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.UUID;

@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
@Import(ActuatorResourceServerConfiguration.class)
@EnableRedisHttpSession
@EnableConfigurationProperties(AuthenticationProperties.class)
public class AuthenticationServiceApplication {

    public static void main(String[] args) {
		SpringApplication.run(AuthenticationServiceApplication.class, args);
	}

    @ConditionalOnProperty(prefix = "configuration", name = "initialLoad", havingValue = "true", matchIfMissing = true)
    @Bean
    CommandLineRunner runner(AuthenticationCommonRepository authenticationCommonRepository,
                             PasswordEncoder passwordEncoder,
                             MongoTemplate mongoTemplate) {
        return args -> {
            if (authenticationCommonRepository.findByEmail(DefaultUsers.SYSTEM_DEFAULT.getValue()) == null) {
                Authentication authentication = Authentication.builder()
                    .email(DefaultUsers.SYSTEM_DEFAULT.getValue())
                    .password(passwordEncoder.encode("noPassword"))
                    .fullName("System Administrator")
                    .enabled(false)
                    .id(UUID.randomUUID().toString())
                    .build();
                log.debug("Creating default authentication: {}", authentication);
                authentication = mongoTemplate.save(authentication, "users_login");
                log.debug("Created Default Authentication: {}", authentication);
            }
        };
    }

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSIONID");
        serializer.setCookiePath("/");
        return serializer;
    }

	@Bean
	public ValidatingMongoEventListener validatingMongoEventListener() {
		return new ValidatingMongoEventListener(validator());
	}

	@Bean
	public LocalValidatorFactoryBean validator() {
		return new LocalValidatorFactoryBean();
	}

	@Primary
	@Bean
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Primary
    @Bean
    public CustomDefaultErrorAttributes customDefaultErrorAttributes() {
        return new CustomDefaultErrorAttributes();
    }

    @Bean
    CorsFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.applyPermitDefaultValues();

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsFilter(source);
    }

    @Bean
    RedisConnectionFactory lettuceConnectionFactory(RedisProperties redisProperties) {
        return new LettuceConnectionFactory(redisProperties.getHost(), redisProperties.getPort());
    }

    @Bean
    RedisTokenStore redisTokenStore(RedisConnectionFactory redisConnectionFactory) {
        return new RedisTokenStore(redisConnectionFactory);
    }

    @Bean
    CustomLogoutSuccessHandler customLogoutSuccessHandler(RedisTokenStoreService redisTokenStoreService) {
        return new CustomLogoutSuccessHandler(redisTokenStoreService);
    }
}

@Slf4j
@Controller
class HomeController {
    @GetMapping("/")
    @ResponseBody
    public String user(@AuthenticationPrincipal org.springframework.security.core.Authentication authentication) {
        return ToStringBuilder.reflectionToString(authentication);
    }
}
