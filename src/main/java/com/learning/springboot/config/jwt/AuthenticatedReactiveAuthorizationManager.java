package com.learning.springboot.config.jwt;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class AuthenticatedReactiveAuthorizationManager implements ReactiveAuthorizationManager<ServerWebExchange> {
    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        if (request.getPath().value().equals("/api/authenticate") && request.getMethod() == HttpMethod.POST) {
            return Mono.just(new AuthorizationDecision(true));
        }
        return authentication
                .map(a -> new AuthorizationDecision(a.isAuthenticated()))
                .defaultIfEmpty(new AuthorizationDecision(false));
    }
}
