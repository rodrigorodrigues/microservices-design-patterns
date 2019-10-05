package com.microservice.jwt.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Validate Authorization Header and if valid return Authentication.
 */
@Slf4j
public class JwtAuthenticationConverter implements ServerAuthenticationConverter {
    private final TokenProvider tokenProvider;

    public JwtAuthenticationConverter(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    private Mono<String> resolveToken(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        log.debug("servletPath: {}", request.getPath());
        String authorizationHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        log.debug("authorizationHeader: {}", authorizationHeader);
        return Mono.justOrEmpty(authorizationHeader)
                .switchIfEmpty(Mono.justOrEmpty(Optional.ofNullable(request.getQueryParams())
                        .map(r -> r.getFirst(HttpHeaders.AUTHORIZATION)))
                )
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
