package com.microservice.authentication;

import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import com.microservice.authentication.common.model.Authentication;
import com.microservice.authentication.common.model.Authority;
import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import com.microservice.authentication.service.CustomLogoutSuccessHandler;
import com.microservice.authentication.service.RedisTokenStoreService;
import com.microservice.web.common.util.constants.DefaultUsers;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.approval.TokenApprovalStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.stereotype.Controller;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@SpringBootApplication
@EnableRedisHttpSession
public class AuthenticationServiceApplication implements WebMvcConfigurer {

    public static void main(String[] args) {
		SpringApplication.run(AuthenticationServiceApplication.class, args);
	}

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/swagger/swagger-ui.html", "/swagger-ui.html");
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
    @ConditionalOnMissingBean
    BuildProperties buildProperties() {
        return new BuildProperties(new Properties());
    }

    @ConditionalOnProperty(prefix = "configuration", name = "initialLoad", havingValue = "true", matchIfMissing = true)
    @Bean
    CommandLineRunner runner(AuthenticationCommonRepository authenticationCommonRepository,
                             PasswordEncoder passwordEncoder,
                             MongoTemplate mongoTemplate) {
        return args -> {
            if (authenticationCommonRepository.findByEmail(DefaultUsers.SYSTEM_DEFAULT.getValue()).isEmpty()) {
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
            if (authenticationCommonRepository.findByEmail("admin@gmail.com").isEmpty()) {
                Authentication authentication = Authentication.builder()
                    .email("admin@gmail.com")
                    .password(passwordEncoder.encode("P@ssword2020!"))
                    .fullName("Admin")
                    .enabled(true)
                    .id(UUID.randomUUID().toString())
                    .authorities(Collections.singletonList(new Authority("ROLE_ADMIN")))
                    .build();
                log.debug("Creating admin authentication: {}", authentication);
                authentication = mongoTemplate.save(authentication, "users_login");
                log.debug("Created admin Authentication: {}", authentication);
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

    @Bean
    CustomLogoutSuccessHandler customLogoutSuccessHandler(RedisTokenStoreService redisTokenStoreService) {
        return new CustomLogoutSuccessHandler(redisTokenStoreService);
    }

    @ConditionalOnMissingBean
    @Bean
    GitProperties gitProperties() {
        return new GitProperties(new Properties());
    }
}

@Slf4j
@Controller
@AllArgsConstructor
class HomeController {
    private final AuthenticationCommonRepository authenticationCommonRepository;

    @GetMapping("/")
    @ResponseBody
    public Authentication user(org.springframework.security.core.Authentication authentication) {
        log.debug("Logged user: {}", authentication);
        Optional<Authentication> findById = authenticationCommonRepository.findById(authentication.getName());
        return findById.orElseGet(() -> authenticationCommonRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found user: " + authentication.getName())));
    }
}
