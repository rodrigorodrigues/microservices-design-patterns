package com.learning.springboot.config;

import com.learning.springboot.config.jwt.CustomReactiveAuthenticationManager;
import com.learning.springboot.config.jwt.JwtAuthenticationConverter;
import com.learning.springboot.config.jwt.TokenProvider;
import com.learning.springboot.service.UserService;
import com.learning.springboot.util.HandleResponseError;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.HttpStatusReturningServerLogoutSuccessHandler;
import org.springframework.security.web.server.authentication.logout.LogoutWebFilter;
import org.springframework.security.web.server.authentication.logout.SecurityContextServerLogoutHandler;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * Spring Security Configuration
 */
@Slf4j
@Configuration
@AllArgsConstructor
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SpringSecurityConfiguration {
    private final UserService userService;

    private final TokenProvider tokenProvider;

    private final HandleResponseError handleResponseError;

    private final WebSessionServerSecurityContextRepository securityContextRepository = new WebSessionServerSecurityContextRepository();

    private static final String[] WHITELIST = {
        // -- swagger ui
        "/v2/api-docs",
        "/swagger-resources",
        "/swagger-resources/**",
        "/configuration/ui",
        "/configuration/security",
        "/swagger-ui.html",
        "/webjars/**",
        "/**/*.js",
        "/**/*.css",
        "/**/*.html",
        "/favicon.ico",
        // other public endpoints of your API may be appended to this array
        "/api/authenticate",
        "/actuator/**"
    };

    @Bean
    public SecurityWebFilterChain configure(ServerHttpSecurity http) {
        return http
            .csrf()
                .csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse())
                .disable()
                .headers()
                .frameOptions().disable()
                .cache().disable()
            .and()
                .formLogin().disable()
                .httpBasic().disable()
                .logout().disable()
                .securityContextRepository(securityContextRepository)
                .authorizeExchange()
                .pathMatchers(WHITELIST).permitAll()
                .anyExchange().authenticated()
            .and()
                .addFilterAt(authenticationWebFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                .addFilterAt(logoutWebFilter(), SecurityWebFiltersOrder.LOGOUT)
                .exceptionHandling()
                .authenticationEntryPoint((exchange, e) -> handleResponseError.handle(exchange, e, true))
            .and()
                .build();
    }

    private AuthenticationWebFilter authenticationWebFilter() {
        AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(reactiveAuthenticationManager());
        authenticationWebFilter.setServerAuthenticationConverter(new JwtAuthenticationConverter(tokenProvider));
        NegatedServerWebExchangeMatcher negateWhiteList = new NegatedServerWebExchangeMatcher(ServerWebExchangeMatchers.pathMatchers(WHITELIST));
        authenticationWebFilter.setRequiresAuthenticationMatcher(negateWhiteList);
        authenticationWebFilter.setAuthenticationFailureHandler((webFilterExchange, exception) -> handleResponseError.handle(webFilterExchange.getExchange(), exception, true));
        authenticationWebFilter.setSecurityContextRepository(securityContextRepository);
        authenticationWebFilter.setAuthenticationSuccessHandler(authenticationSuccessHandler());
        return authenticationWebFilter;
    }

    private ServerAuthenticationSuccessHandler authenticationSuccessHandler() {
        return (webFilterExchange, authentication) -> webFilterExchange.getChain().filter(webFilterExchange.getExchange())
                .subscriberContext(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }

    private LogoutWebFilter logoutWebFilter() {
        SecurityContextServerLogoutHandler logoutHandler = new SecurityContextServerLogoutHandler();
        logoutHandler.setSecurityContextRepository(securityContextRepository);

        LogoutWebFilter logoutWebFilter = new LogoutWebFilter();
        logoutWebFilter.setLogoutHandler(logoutHandler);
        logoutWebFilter.setLogoutSuccessHandler(new HttpStatusReturningServerLogoutSuccessHandler(HttpStatus.OK));
        logoutWebFilter.setRequiresLogoutMatcher(ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, "/logout"));
        return logoutWebFilter;
    }

    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager() {
        return new CustomReactiveAuthenticationManager(userService);
    }

    @Bean
    CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.applyPermitDefaultValues();

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }

}
