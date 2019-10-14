package com.microservice.authentication.config;

import com.microservice.authentication.dto.JwtTokenDto;
import com.microservice.jwt.common.JwtAuthenticationConverter;
import com.microservice.jwt.common.TokenProvider;
import com.microservice.web.common.util.HandleResponseError;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * Spring Security Configuration
 */
@Slf4j
@Configuration
@AllArgsConstructor
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SpringSecurityConfiguration {
    private final HandleResponseError handleResponseError;

    private final TokenProvider tokenProvider;

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
        "/api/logout",
        "/actuator/**"
    };

    @Bean
    public SecurityWebFilterChain configure(ServerHttpSecurity http) {
        return http
            .authorizeExchange()
                .pathMatchers(WHITELIST).permitAll()
            .anyExchange().authenticated()
            .and()
            .formLogin()
                .loginPage("/api/authenticate")
                .authenticationFailureHandler((webFilterExchange, exception) -> handleResponseError.handle(webFilterExchange.getExchange(), exception, true))
                .authenticationSuccessHandler(getServerAuthenticationSuccessHandler())
                .securityContextRepository(webSessionServerSecurityContextRepository())
                .and().logout().logoutUrl("/api/logout")
                .and().httpBasic().disable()
            .addFilterAt(authenticationWebFilter(), SecurityWebFiltersOrder.AUTHORIZATION)
            .exceptionHandling()
                .authenticationEntryPoint((exchange, e) -> handleResponseError.handle(exchange, e, true))
            .and()
            .requestCache()
                .requestCache(NoOpServerRequestCache.getInstance())
            .and()
            .csrf()
                .disable()
            .headers()
                .frameOptions().disable()
            .cache().disable()
            .and()
            .build();
    }

    @Bean
    public WebSessionServerSecurityContextRepository webSessionServerSecurityContextRepository() {
        return new WebSessionServerSecurityContextRepository();
    }

    private AuthenticationWebFilter authenticationWebFilter() {
        AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(Mono::just);
        authenticationWebFilter.setServerAuthenticationConverter(new JwtAuthenticationConverter(tokenProvider));
        NegatedServerWebExchangeMatcher negateWhiteList = new NegatedServerWebExchangeMatcher(ServerWebExchangeMatchers.pathMatchers(WHITELIST));
        authenticationWebFilter.setRequiresAuthenticationMatcher(negateWhiteList);
        return authenticationWebFilter;
    }

    private ServerAuthenticationSuccessHandler getServerAuthenticationSuccessHandler() {
        return (webFilterExchange, authentication) -> {
            ServerWebExchange exchange = webFilterExchange.getExchange();
            //MultiValueMap<String, String> body = webFilterExchange.getExchange().getFormData().block();
            log.debug("getServerAuthenticationSuccessHandler:authentication: {}", authentication);
            log.debug("webFilterExchange.getChain(): {}", webFilterExchange.getChain());
            ServerHttpResponse response = exchange.getResponse();
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            String authorization = "Bearer " + tokenProvider.createToken(authentication, authentication.getName(), false);
            JwtTokenDto jwtToken = new JwtTokenDto(authorization);
            response.getHeaders().add(HttpHeaders.AUTHORIZATION, authorization);
            response.setStatusCode(HttpStatus.OK);
            response.writeWith(Mono.just(response.bufferFactory().wrap(covertToByteArray(jwtToken))));
            return webFilterExchange.getChain().filter(exchange);
        };
    }

    private byte[] covertToByteArray(JwtTokenDto jwtToken) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(jwtToken);
            oos.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
