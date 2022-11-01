package com.microservice.authentication;

import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import com.microservice.authentication.common.model.Authentication;
import com.microservice.authentication.common.model.Authority;
import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
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
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
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
        serializer.setUseBase64Encoding(false);
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
    LogoutSuccessHandler customLogoutSuccessHandler() {
        return new SimpleUrlLogoutSuccessHandler();
    }

    @ConditionalOnMissingBean
    @Bean
    GitProperties gitProperties() {
        return new GitProperties(new Properties());
    }

    @ConditionalOnMissingBean
    @Bean
    RedisTokenStoreService redisTokenStoreService(DefaultTokenServices defaultTokenServices) {
        return new RedisTokenStoreService() {
            @Override
            public OAuth2AccessToken generateToken(OAuth2Authentication oAuth2Authentication) {
                log.debug("Created new token for: {}", oAuth2Authentication.getName());
                return defaultTokenServices.createAccessToken(oAuth2Authentication);
            }

            @Override
            public void removeAllTokensByAuthenticationUser(org.springframework.security.core.Authentication authentication) {
            }

            @Override
            public OAuth2AccessToken refreshToken(TokenRequest tokenRequest) {
                return defaultTokenServices.refreshAccessToken(tokenRequest.getRequestParameters().get("refresh_token"), tokenRequest);
            }

            @Override
            public OAuth2AccessToken getToken(org.springframework.security.core.Authentication authentication) {
                OAuth2Request oAuth2Request = new OAuth2Request(null, authentication.getName(), authentication.getAuthorities(),
                    true, Collections.singleton("read"), null, null, null, null);
                OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(oAuth2Request, authentication);
                return generateToken(oAuth2Authentication);
            }
        };
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
