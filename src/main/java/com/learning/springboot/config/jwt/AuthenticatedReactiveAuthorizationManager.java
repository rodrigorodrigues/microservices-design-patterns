package com.learning.springboot.config.jwt;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class AuthenticatedReactiveAuthorizationManager implements ReactiveAuthorizationManager<ServerWebExchange> {
    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, ServerWebExchange object) {
        return authentication
                .map(a -> new AuthorizationDecision(a.isAuthenticated()))
                .defaultIfEmpty(new AuthorizationDecision(false));
    }
}
