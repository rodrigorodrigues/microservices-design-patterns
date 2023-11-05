package com.springboot.edgeserver.config;

import com.springboot.edgeserver.util.HandleResponseError;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler;

/**
 * Spring Security Configuration
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class EdgeServerWebSecurityConfiguration {
    private final HandleResponseError handleResponseError;

//    private final DefaultTokenServices defaultTokenServices;

    private final TokenStore tokenStore;

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
            "/actuator/gateway/**",
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

    public EdgeServerWebSecurityConfiguration(HandleResponseError handleResponseError, TokenStore tokenStore) {
        this.handleResponseError = handleResponseError;
        this.tokenStore = tokenStore;
    }

    @Bean
    public SecurityWebFilterChain configure(ServerHttpSecurity http) {
        return http
                .csrf(c -> c.disable().headers(h -> h.frameOptions(f -> f.disable().cache(ServerHttpSecurity.HeaderSpec.CacheSpec::disable))))
/*
                .csrf(c -> c.csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new ServerCsrfTokenRequestAttributeHandler()))
*/
                .authorizeExchange(a -> a.pathMatchers(WHITELIST).permitAll()
                        .pathMatchers("/actuator/**").hasRole("ADMIN")
                        .anyExchange().authenticated())
                .formLogin(c -> c.authenticationSuccessHandler((webFilterExchange, authentication) -> webFilterExchange.getChain()
                                .filter(webFilterExchange.getExchange())
                                .contextWrite(context -> ReactiveSecurityContextHolder.withAuthentication(authentication)))
                        .authenticationFailureHandler((webFilterExchange, exception) -> handleResponseError.handle(webFilterExchange.getExchange(), exception, true)))

                .logout(l -> l.logoutSuccessHandler(new RedirectServerLogoutSuccessHandler() {
                    @Override
                    public Mono<Void> onLogoutSuccess(WebFilterExchange exchange, Authentication authentication) {
                        log.info("Logout success! authType: {}", authentication.getClass().getName());
                        if (authentication instanceof OAuth2AuthenticationToken) {
                            tokenStore.findTokensByClientId(authentication.getName())
                                    .forEach(tokenStore::removeAccessToken);
                        }
                        return super.onLogoutSuccess(exchange, authentication);
                    }
                }))
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
}