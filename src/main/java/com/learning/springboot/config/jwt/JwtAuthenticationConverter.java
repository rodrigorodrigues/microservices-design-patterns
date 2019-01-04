package com.learning.springboot.config.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Filters incoming requests and installs a Spring Security principal if a header corresponding to a valid user is
 * found.
 */
@Slf4j
public class JwtAuthenticationConverter implements ServerAuthenticationConverter {
    private final TokenProvider tokenProvider;

    public JwtAuthenticationConverter(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    private Mono<String> resolveToken(ServerWebExchange exchange) {
        log.debug("servletPath: {}", exchange.getRequest().getPath());
        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .filter(t -> t.startsWith("Bearer "))
                .map(t -> t.substring(7));
    }

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        return resolveToken(exchange)
                .filter(tokenProvider::validateToken)
                .map(tokenProvider::getAuthentication);
    }

}
