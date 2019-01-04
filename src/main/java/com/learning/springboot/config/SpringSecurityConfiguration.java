package com.learning.springboot.config;


import com.learning.springboot.config.jwt.AuthenticatedReactiveAuthorizationManager;
import com.learning.springboot.config.jwt.JwtAuthenticationConverter;
import com.learning.springboot.config.jwt.TokenProvider;
import com.learning.springboot.service.PersonService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authorization.AuthorizationWebFilter;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.ResponseStatusException;

/**
 * Spring Security Configuration
 */
@Slf4j
@Configuration
@AllArgsConstructor
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SpringSecurityConfiguration {
    private final PersonService personService;

    private final TokenProvider tokenProvider;

    private static final String[] AUTH_WHITELIST = {
            // -- swagger ui
            "/v2/api-docs",
            "/swagger-resources",
            "/swagger-resources/**",
            "/configuration/ui",
            "/configuration/security",
            "/swagger-ui.html",
            "/webjars/**",
            // other public endpoints of your API may be appended to this array
            "/api/authenticate"
    };

    @Bean
    public SecurityWebFilterChain configure(ServerHttpSecurity http) {
        return http
            .csrf()
                .disable()
                .headers()
                .frameOptions()
                .disable()
            .and()
                .authorizeExchange()
                .pathMatchers(AUTH_WHITELIST).permitAll()
                .anyExchange().authenticated()
            .and()
                .addFilterAt(authenticationWebFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                //.addFilterAt(authorizationWebFilter(), SecurityWebFiltersOrder.AUTHORIZATION)
                .httpBasic().disable()
                .formLogin().disable()
                .build();
    }

    private AuthorizationWebFilter authorizationWebFilter() {
        return new AuthorizationWebFilter(new AuthenticatedReactiveAuthorizationManager());
    }


    private AuthenticationWebFilter authenticationWebFilter() {
        AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(authenticationManager());
        authenticationWebFilter.setServerAuthenticationConverter(new JwtAuthenticationConverter(tokenProvider));
        NegatedServerWebExchangeMatcher negateWhiteList = new NegatedServerWebExchangeMatcher(ServerWebExchangeMatchers.pathMatchers(AUTH_WHITELIST));
        authenticationWebFilter.setRequiresAuthenticationMatcher(negateWhiteList);
        authenticationWebFilter.setSecurityContextRepository(new WebSessionServerSecurityContextRepository());
        authenticationWebFilter.setAuthenticationFailureHandler((webFilterExchange, exception) -> {
            HttpStatus httpStatus = HttpStatus.UNAUTHORIZED;
            if (exception instanceof AuthenticationCredentialsNotFoundException) {
                httpStatus = HttpStatus.FORBIDDEN;
            }
            throw new ResponseStatusException(httpStatus, ExceptionUtils.getMessage(exception));
        });
        return authenticationWebFilter;
    }

    @Bean
    public ReactiveAuthenticationManager authenticationManager() {
        return new UserDetailsRepositoryReactiveAuthenticationManager(personService);
    }
/*
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:8080"));
        configuration.setAllowedMethods(Collections.singletonList("GET"));
        configuration.setAllowedHeaders(Collections.singletonList("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }*/

}
