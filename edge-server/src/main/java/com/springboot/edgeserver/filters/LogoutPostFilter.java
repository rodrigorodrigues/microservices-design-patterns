package com.springboot.edgeserver.filters;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;

/**
 * Filter applied after logout to invalidade the session.
 */
@Slf4j
@Component
public class LogoutPostFilter implements GatewayFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
                .then(exchange.getSession())
                .flatMap(WebSession::invalidate)
                .doOnSuccess(c -> log.debug("Invalid session after logout"));
    }
}
