package com.springboot.edgeserver.config;

import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.edgeserver.util.HandleResponseError;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Spring Security Configuration
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class EdgeServerWebSecurityConfiguration implements BeanClassLoaderAware {
    private final HandleResponseError handleResponseError;

    private final DefaultTokenServices defaultTokenServices;

    private final RedisTokenStore redisTokenStore;

    private static final String[] WHITELIST = {
            // -- swagger ui
            "/v2/api-docs",
            "/swagger-resources",
            "/swagger-resources/**",
            "/configuration/ui",
            "/configuration/security",
            "/swagger-ui.html",
            "/webjars/**",
            "/*.js",
            "/*.css",
            "/*.html",
            "/favicon.ico",
            // other public endpoints of your API may be appended to this array
            "/actuator/info",
            "/actuator/health",
            "/actuator/prometheus",
            "/error",
            "/api/**",
            "/oauth2/**",
            "/.well-known/jwks.json",
            "/swagger/**",
            "/login",
            "/grafana/**"
    };

    private ClassLoader loader;

    public EdgeServerWebSecurityConfiguration(HandleResponseError handleResponseError, DefaultTokenServices defaultTokenServices, RedisTokenStore redisTokenStore) {
        this.handleResponseError = handleResponseError;
        this.defaultTokenServices = defaultTokenServices;
        this.redisTokenStore = redisTokenStore;
    }

    @Bean
    public SecurityWebFilterChain configure(ServerHttpSecurity http) {
        return http
                .csrf()
                .disable()
                .headers()
                .frameOptions().disable()
                .cache().disable()
                .and()
                .authorizeExchange()
                .pathMatchers(WHITELIST).permitAll()
                .pathMatchers("/actuator/**").hasRole("ADMIN")
                .anyExchange().authenticated()
                .and()
                .formLogin()
                .authenticationSuccessHandler((webFilterExchange, authentication) -> {
                    OAuth2Request oAuth2Request = new OAuth2Request(null, authentication.getName(), authentication.getAuthorities(),
                            true, Collections.singleton("read"), null, null, null, null);
                    OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(oAuth2Request, authentication);
                    defaultTokenServices.createAccessToken(oAuth2Authentication);
                    return webFilterExchange.getChain().filter(webFilterExchange.getExchange())
                            .subscriberContext(ReactiveSecurityContextHolder.withAuthentication(authentication));
                })
                .authenticationFailureHandler((webFilterExchange, exception) -> handleResponseError.handle(webFilterExchange.getExchange(), exception, true))
                .and()
                .logout()
                .and()
//                .addFilterAt(authenticationWebFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                .exceptionHandling()
//                .authenticationEntryPoint((exchange, e) -> handleResponseError.handle(exchange, e, true))
                .and()
                .build();
    }

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new GenericJackson2JsonRedisSerializer(objectMapper());
    }

    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules(SecurityJackson2Modules.getModules(this.loader));
        return mapper;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.loader = classLoader;
    }
}