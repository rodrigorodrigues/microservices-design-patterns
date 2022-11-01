package com.springboot.edgeserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.edgeserver.util.HandleResponseError;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;

/**
 * Spring Security Configuration
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class EdgeServerWebSecurityConfiguration implements BeanClassLoaderAware {
    private final HandleResponseError handleResponseError;

//    private final DefaultTokenServices defaultTokenServices;

    private final RedisTokenStore redisTokenStore;

    private static final String[] WHITELIST = {
            // -- swagger ui
            "/v3/api-docs/**",
            "/swagger-resources",
            "/swagger-resources/**",
            "/swagger/**",
            "/swagger-ui/**",
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
            "/actuator/health/**",
            "/actuator/prometheus",
            "/error",
            "/api/**",
            "/oauth2/**",
            "/.well-known/jwks.json",
            "/swagger/**",
            "/login",
            "/admin/**"
    };

    private ClassLoader loader;

    public EdgeServerWebSecurityConfiguration(HandleResponseError handleResponseError, RedisTokenStore redisTokenStore) {
        this.handleResponseError = handleResponseError;
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
                .authenticationSuccessHandler((webFilterExchange, authentication) -> webFilterExchange.getChain()
                        .filter(webFilterExchange.getExchange())
                        .subscriberContext(ReactiveSecurityContextHolder.withAuthentication(authentication)))
                .authenticationFailureHandler((webFilterExchange, exception) -> handleResponseError.handle(webFilterExchange.getExchange(), exception, true))
                .and()
                .logout()
                .logoutSuccessHandler(new RedirectServerLogoutSuccessHandler() {
                    @Override
                    public Mono<Void> onLogoutSuccess(WebFilterExchange exchange, Authentication authentication) {
                        log.info("Logout success! authType: {}", authentication.getClass().getName());
                        if (authentication instanceof OAuth2AuthenticationToken) {
                            redisTokenStore.findTokensByClientId(authentication.getName())
                                    .forEach(redisTokenStore::removeAccessToken);
                        }
                        return super.onLogoutSuccess(exchange, authentication);
                    }
                })
                .and()
                .build();
    }

    /*private Mono<Void> processAuthentication(WebFilterExchange webFilterExchange, Authentication authentication) {
        return webFilterExchange.getExchange().getSession()
                        .map(s -> {
                            SecurityContextImpl securityContext = new SecurityContextImpl(authentication);
                            s.getAttributes().put(DEFAULT_SPRING_SECURITY_CONTEXT_ATTR_NAME, securityContext);
                            OAuth2AccessToken accessToken;
                            if (authentication instanceof OAuth2Authentication oAuth2Authentication) {
                                accessToken = defaultTokenServices.createAccessToken(oAuth2Authentication);
                            } else {
                                OAuth2Request oAuth2Request = new OAuth2Request(null, authentication.getName(), authentication.getAuthorities(),
                                        true, Collections.singleton("read"), null, null, null, null);
                                OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(oAuth2Request, authentication);
                                accessToken = defaultTokenServices.createAccessToken(oAuth2Authentication);
                            }
                            log.info("login:set accessToken: {}={}", accessToken.getTokenType(), accessToken.getExpiration());
                            return securityContext;
                        })
                .subscriberContext(ReactiveSecurityContextHolder.withAuthentication(authentication))
                .doOnSuccess(c -> log.info("login:Authenticated user in the session: {}", c.getAuthentication().getName()))
                .then();
    }*/

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new GenericJackson2JsonRedisSerializer(objectMapper());
    }

    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules(SecurityJackson2Modules.getModules(this.loader));
        mapper.registerModule(new OAuth2AuthenticationJackson2Module());
        mapper.registerModule(new OAuth2RequestModule());
        return mapper;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.loader = classLoader;
    }
}