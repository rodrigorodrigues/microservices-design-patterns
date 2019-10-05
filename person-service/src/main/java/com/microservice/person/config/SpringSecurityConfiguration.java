package com.microservice.person.config;

import com.microservice.jwt.common.JwtAuthenticationConverter;
import com.microservice.jwt.common.TokenProvider;
import com.microservice.web.common.util.HandleResponseError;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import reactor.core.publisher.Mono;

/**
 * Spring Security Configuration
 */
@Configuration
@AllArgsConstructor
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SpringSecurityConfiguration {
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
        "/actuator/**"
    };

    @Bean
    public SecurityWebFilterChain configure(ServerHttpSecurity http) {
        return http
            .csrf()
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
                .exceptionHandling()
                .authenticationEntryPoint((exchange, e) -> handleResponseError.handle(exchange, e, true))
            .and()
                .build();
    }

    private AuthenticationWebFilter authenticationWebFilter() {
        AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(Mono::just);
        authenticationWebFilter.setServerAuthenticationConverter(new JwtAuthenticationConverter(tokenProvider));
        NegatedServerWebExchangeMatcher negateWhiteList = new NegatedServerWebExchangeMatcher(ServerWebExchangeMatchers.pathMatchers(WHITELIST));
        authenticationWebFilter.setRequiresAuthenticationMatcher(negateWhiteList);
        authenticationWebFilter.setAuthenticationFailureHandler((webFilterExchange, exception) -> handleResponseError.handle(webFilterExchange.getExchange(), exception, true));
        authenticationWebFilter.setSecurityContextRepository(securityContextRepository);
        authenticationWebFilter.setAuthenticationSuccessHandler((webFilterExchange, authentication) -> webFilterExchange.getChain().filter(webFilterExchange.getExchange())
            .subscriberContext(ReactiveSecurityContextHolder.withAuthentication(authentication)));
        return authenticationWebFilter;
    }

}
