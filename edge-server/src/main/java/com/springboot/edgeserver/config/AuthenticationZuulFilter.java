package com.springboot.edgeserver.config;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

@Slf4j
@Component
public class AuthenticationZuulFilter implements GatewayFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .filter(c -> {
                    boolean authenticated = c.getAuthentication().isAuthenticated();
                    log.debug("User is authenticated? {}", authenticated);
                    return authenticated;
                })
                .map(c -> {
                    Authentication authentication = c.getAuthentication();
                    log.debug("authenticationZuulFilter: {}", authentication);
                    if (authentication == null || !authentication.isAuthenticated()) {
                        log.debug("User is not authenticated: {}", (authentication != null ? authentication.getName() : ""));
                        String message = String.format("To access(%s) user must be authenticated!", exchange.getRequest().getURI());
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
                    } else if (authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch("ROLE_ADMIN"::equals)) {
                        log.debug("User has admin role: {}", authentication.getAuthorities());
                        exchange.getRequest().mutate().header("X-WEBAUTH-USER", "admin");
                    } else {
                        String message = String.format("User has not ROLE_ADMIN: %s", authentication.getName());
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
                    }
                    return chain.filter(exchange);
            })
                .switchIfEmpty(Mono.error(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated")))
                .then(chain.filter(exchange));
    }

}