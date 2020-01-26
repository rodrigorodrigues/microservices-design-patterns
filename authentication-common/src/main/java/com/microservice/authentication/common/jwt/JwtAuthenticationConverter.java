package com.microservice.authentication.common.jwt;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Validate Authorization Header and if valid return Authentication.
 */
@Slf4j
@AllArgsConstructor
public class JwtAuthenticationConverter implements ServerAuthenticationConverter {
    private final TokenStore tokenStore;

    private Mono<String> resolveToken(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        log.debug("servletPath: {}", request.getPath());
        String authorizationHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        log.debug("authorizationHeader: {}", authorizationHeader);
        return Mono.justOrEmpty(authorizationHeader)
                .switchIfEmpty(Mono.justOrEmpty(Optional.of(request.getQueryParams())
                        .map(r -> r.getFirst(HttpHeaders.AUTHORIZATION)))
                )
                .filter(t -> t.toLowerCase().startsWith("bearer "))
                .map(t -> t.substring(7));
    }

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        return resolveToken(exchange)
                .map(tokenStore::readAuthentication)
                .map(OAuth2Authentication::getUserAuthentication);
    }

}
