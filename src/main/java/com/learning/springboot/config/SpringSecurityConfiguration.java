package com.learning.springboot.config;


import com.learning.springboot.config.jwt.CustomReactiveAuthenticationManager;
import com.learning.springboot.config.jwt.JwtAuthenticationConverter;
import com.learning.springboot.config.jwt.TokenProvider;
import com.learning.springboot.service.PersonService;
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
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.logout.HttpStatusReturningServerLogoutSuccessHandler;
import org.springframework.security.web.server.authentication.logout.LogoutWebFilter;
import org.springframework.security.web.server.authentication.logout.SecurityContextServerLogoutHandler;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

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
        // other public endpoints of your API may be appended to this array
        "/api/authenticate"
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
            .build();
    }

    private AuthenticationWebFilter authenticationWebFilter() {
        AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(reactiveAuthenticationManager());
        authenticationWebFilter.setServerAuthenticationConverter(new JwtAuthenticationConverter(tokenProvider));
        NegatedServerWebExchangeMatcher negateWhiteList = new NegatedServerWebExchangeMatcher(ServerWebExchangeMatchers.pathMatchers(WHITELIST));
        authenticationWebFilter.setRequiresAuthenticationMatcher(negateWhiteList);
        authenticationWebFilter.setAuthenticationFailureHandler((webFilterExchange, exception) -> handleResponseError.handle(webFilterExchange.getExchange(), exception, true));
        authenticationWebFilter.setSecurityContextRepository(securityContextRepository);
        return authenticationWebFilter;
    }

    private LogoutWebFilter logoutWebFilter() {
        LogoutWebFilter logoutWebFilter = new LogoutWebFilter();

        SecurityContextServerLogoutHandler logoutHandler = new SecurityContextServerLogoutHandler();
        logoutHandler.setSecurityContextRepository(securityContextRepository);

        logoutWebFilter.setLogoutHandler(logoutHandler);
        logoutWebFilter.setLogoutSuccessHandler(new HttpStatusReturningServerLogoutSuccessHandler(HttpStatus.OK));
        logoutWebFilter.setRequiresLogoutMatcher(ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, "/logout"));

        return logoutWebFilter;
    }

    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager() {
        return new CustomReactiveAuthenticationManager(personService);
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
