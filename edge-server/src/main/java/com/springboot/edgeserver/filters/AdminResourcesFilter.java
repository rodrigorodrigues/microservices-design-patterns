package com.springboot.edgeserver.filters;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.session.web.server.session.SpringSessionWebSessionStore;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;

import static org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository.DEFAULT_SPRING_SECURITY_CONTEXT_ATTR_NAME;

/**
 * Filter for /admin/** to check if user is authenticated and has role ADMIN.
 */
@Slf4j
@Component
@AllArgsConstructor
public class AdminResourcesFilter implements GatewayFilter {
    private final SpringSessionWebSessionStore springSessionWebSessionStore;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getSession()
                .flatMap(session -> {
                    String sessionId = exchange.getResponse().getHeaders().getFirst("sessionId");
                    log.debug("sessionId:header: {}", sessionId);
                    log.debug("sessionId:session {}", session.getId());
                    log.debug("Trying to validate path: {}", exchange.getRequest().getPath());
                    log.debug("Trying first to get securityContext by session:size: {}", session.getAttributes().size());
                    return Mono.justOrEmpty((SecurityContext) session.getAttribute(DEFAULT_SPRING_SECURITY_CONTEXT_ATTR_NAME));
                })
                .switchIfEmpty(ReactiveSecurityContextHolder.getContext())
                .switchIfEmpty(Mono.error(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated")))
                .flatMap(securityContext -> {
                    Authentication authentication = securityContext.getAuthentication();
                    if (authentication == null || !authentication.isAuthenticated()) {
                        log.debug("User is not authenticated: {}", (authentication != null ? authentication.getName() : ""));
                        String message = String.format("To access this resource(%securityContext) user must be authenticated!", exchange.getRequest().getURI());
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, message));
                    } else if (authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch("ROLE_ADMIN"::equals)) {
                        log.debug("User has admin role: {}", authentication.getAuthorities());
                        if (!exchange.getRequest().getHeaders().containsHeader(HttpHeaders.AUTHORIZATION)) {
                            return exchange.getSession()
                                    .flatMap(session -> {
                                        log.info("adminResourcesFilter:Set authorization header from redis session: {}", session.getId());
                                        return springSessionWebSessionStore.retrieveSession(session.getId())
                                                .flatMap(s -> {
                                                    WebSession webSession = (WebSession) s;
                                                    OAuth2AccessToken accessToken = webSession.getAttribute("token");
                                                    log.debug("accessToken: {}", accessToken);
                                                    if (accessToken != null) {
                                                        log.debug("Set token from session: {}={}", session.getId(), accessToken.getTokenValue());
                                                        ServerHttpRequest builder = exchange.getRequest().mutate()
                                                                .header("X-WEBAUTH-USER", "admin")
                                                                .header(HttpHeaders.AUTHORIZATION, String.format("%s %s", accessToken.getTokenType()
                                                                        .getValue(), accessToken.getTokenValue()))
                                                                .build();
                                                        return chain.filter(exchange.mutate()
                                                                .request(builder).build());
                                                    } else {
                                                        return chain.filter(exchange);
                                                    }
                                                }).switchIfEmpty(springSessionWebSessionStore.createWebSession().then().doOnSuccess(c -> log.debug("Created new session: {}", session.getId())));
                            });
                        }
                        return chain.filter(exchange);
                    } else {
                        String message = String.format("User has not ROLE_ADMIN: %securityContext", authentication.getName());
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, message));
                    }
                });

    }
}