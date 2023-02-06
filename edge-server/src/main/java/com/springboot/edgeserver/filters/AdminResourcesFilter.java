package com.springboot.edgeserver.filters;

import java.util.Comparator;
import java.util.Optional;

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
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import static org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository.DEFAULT_SPRING_SECURITY_CONTEXT_ATTR_NAME;

/**
 * Filter for /admin/** to check if user is authenticated and has role ADMIN.
 */
@Slf4j
@Component
@AllArgsConstructor
public class AdminResourcesFilter implements GatewayFilter {
    private final TokenStore tokenStore;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getSession()
                .flatMap(session -> {
                    log.debug("Trying to validate path: {}", exchange.getRequest().getPath());
                    log.debug("Trying first to get securityContext by session:size: {}", session.getAttributes().size());
                    return Mono.justOrEmpty((SecurityContext) session.getAttribute(DEFAULT_SPRING_SECURITY_CONTEXT_ATTR_NAME));
                })
                .switchIfEmpty(ReactiveSecurityContextHolder.getContext())
                .switchIfEmpty(Mono.error(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated")))
                .flatMap(s -> {
                    Authentication authentication = s.getAuthentication();
                    if (authentication == null || !authentication.isAuthenticated()) {
                        log.debug("User is not authenticated: {}", (authentication != null ? authentication.getName() : ""));
                        String message = String.format("To access this resource(%s) user must be authenticated!", exchange.getRequest().getURI());
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, message));
                    } else if (authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch("ROLE_ADMIN"::equals)) {
                        log.debug("User has admin role: {}", authentication.getAuthorities());
                        if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                            Optional<OAuth2AccessToken> oAuth2AccessToken = tokenStore.findTokensByClientId(authentication.getName())
                                    .stream()
                                    .filter(a -> !a.isExpired())
                                    .max(Comparator.comparing(OAuth2AccessToken::getExpiration));

                            if (oAuth2AccessToken.isPresent()) {
                                log.info("adminResourcesFilter:Set authorization header from redis session");
                                OAuth2AccessToken oAuth2AccessTokenValue = oAuth2AccessToken.get();
                                ServerHttpRequest builder = exchange.getRequest().mutate()
                                        .header("X-WEBAUTH-USER", "admin")
                                        .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", oAuth2AccessTokenValue.getValue()))
                                        .build();
                                return chain.filter(exchange.mutate().request(builder).build());
                            }
                        }
                        return chain.filter(exchange);
                    } else {
                        String message = String.format("User has not ROLE_ADMIN: %s", authentication.getName());
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, message));
                    }
                });

    }

}