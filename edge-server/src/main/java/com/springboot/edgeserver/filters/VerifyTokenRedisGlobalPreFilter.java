package com.springboot.edgeserver.filters;

import java.util.Comparator;
import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

/**
 * Verify if the bearer token still exists on Redis before processing the request.
 */
@Slf4j
@AllArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class VerifyTokenRedisGlobalPreFilter implements GlobalFilter {
    private final RedisTokenStore redisTokenStore;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        RequestPath path = exchange.getRequest().getPath();
        if (StringUtils.startsWithAny(path.value(), "/admin", "/api/logout", "/login/oauth2/", "/oauth2/",
                "/api/authenticate", "/api/authenticatedUser", "/oauth/", "/swagger/", "/swagger/", "/swagger-ui/", "/.well-known/jwks.json")) {
            log.debug("Skip token redis validation for following path: {}", path);
            String authorizationHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (StringUtils.isBlank(authorizationHeader)) {
                log.debug("Trying to set authorization header if user is authenticated.");
                return ReactiveSecurityContextHolder.getContext()
                        .flatMap(s -> {
                            Authentication authentication = s.getAuthentication();
                            if (authentication != null && authentication.isAuthenticated()) {
                                log.debug("User is authenticated:");
                                Optional<OAuth2AccessToken> oAuth2AccessToken = redisTokenStore.findTokensByClientId(authentication.getName()).stream()
                                        .filter(a -> !a.isExpired())
                                        .sorted(Comparator.comparing(OAuth2AccessToken::getExpiration).reversed())
                                        .findFirst();
                                if (oAuth2AccessToken.isPresent()) {
                                    log.debug("verifyTokenRedis:Set authorization header from redis session");
                                    OAuth2AccessToken oAuth2AccessTokenValue = oAuth2AccessToken.get();
                                    ServerHttpRequest build = exchange.getRequest().mutate()
                                            .header(HttpHeaders.AUTHORIZATION, String.format("%s %s", oAuth2AccessTokenValue.getTokenType(), oAuth2AccessTokenValue.getValue()))
                                            .build();
                                    return chain.filter(exchange.mutate().request(build).build());
                                }
                                log.debug("Not found oAuth2AccessToken: {}", oAuth2AccessToken.isPresent());
                            }
                            return chain.filter(exchange);
                        })
                        .switchIfEmpty(chain.filter(exchange));
            } else {
                return chain.filter(exchange);
            }
        }
        log.debug("Validating token against redis: {}", path);
        String authorizationHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.isBlank(authorizationHeader)) {
            log.debug("Authorization Header not found reject the request!");
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization Header not found."));
        }
        OAuth2Authentication oAuth2Authentication = redisTokenStore.readAuthentication(authorizationHeader.replaceFirst("(?i)Bearer ", ""));
        if (oAuth2Authentication == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token not found in redis."));
        }
        log.debug("Found valid redis for a given token");
        return chain.filter(exchange);
    }
}
