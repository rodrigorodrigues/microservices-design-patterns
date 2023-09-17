package com.springboot.edgeserver.filters;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.BaggageInScope;
import io.micrometer.tracing.Tracer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR;

/**
 * Verify if the bearer token still exists on Redis before processing the request.
 */
@Slf4j
@AllArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class VerifyTokenRedisGlobalPreFilter implements GlobalFilter {
    private final TokenStore tokenStore;

    private final Tracer tracer;

    private final ObjectMapper objectMapper;

    private final List<HttpMessageReader<?>> messageReaders = HandlerStrategies.withDefaults().messageReaders();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        RequestPath path = request.getPath();
        if (StringUtils.startsWithAny(path.value(), "/admin", "/api/logout", "/login/oauth2/", "/oauth2/",
                "/api/authenticate", "/api/authenticatedUser", "/oauth/", "/swagger/", "/swagger-ui/", "/.well-known/jwks.json",
                "/v3/api-docs")) {
            log.debug("Skip token redis validation for following path: {}", path);
            String authorizationHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (StringUtils.isBlank(authorizationHeader)) {
                log.debug("Trying to set authorization header if user is authenticated.");
                return ReactiveSecurityContextHolder.getContext()
                        .flatMap(s -> {
                            Authentication authentication = s.getAuthentication();
                            if (authentication != null && authentication.isAuthenticated()) {
                                log.debug("User is authenticated:");
                                Optional<OAuth2AccessToken> oAuth2AccessToken = tokenStore.findTokensByClientId(authentication.getName()).stream()
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
        String authorizationHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.isBlank(authorizationHeader)) {
            log.debug("Authorization Header not found reject the request!");
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization Header not found."));
        }
        OAuth2Authentication oAuth2Authentication = tokenStore.readAuthentication(authorizationHeader.replaceFirst("(?i)Bearer ", ""));
        if (oAuth2Authentication == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token not found in redis."));
        }
        log.debug("Found valid redis for a given token");
        if (request.getMethod() != HttpMethod.POST) {
            return chain.filter(exchange);
        } else {
            return addRequestIdHeader(chain, exchange);
        }
    }

    private Mono<Void> addRequestIdHeader(GatewayFilterChain chain, ServerWebExchange exchange) {
        return ServerWebExchangeUtils.cacheRequestBodyAndRequest(exchange, (serverHttpRequest) -> {
            final ServerRequest serverRequest = ServerRequest
                .create(exchange.mutate().request(serverHttpRequest).build(), messageReaders);
            return serverRequest.bodyToMono(String.class).doOnNext(objectValue -> {
                exchange.getAttributes().put(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR, objectValue);
            }).then(Mono.defer(() -> {
                ServerHttpRequest cachedRequest = exchange
                        .getAttribute(CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR);
                Assert.notNull(cachedRequest, "cache request shouldn't be null");
                String objectValue = exchange.getAttribute(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR);
                log.debug("objectValue: {}", objectValue);
                if (StringUtils.isNotBlank(objectValue)) {
                    String requestId = getRequestIdFromPayload(objectValue);
                    if (StringUtils.isNotBlank(requestId)) {
                        log.info("Found requestId: {}", requestId);
                        try (BaggageInScope baggage = tracer.createBaggageInScope("requestId", requestId)) {
                            serverHttpRequest.mutate().header("requestId", baggage.get());
                        }
                    }
                }
                exchange.getAttributes().remove(CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR);
                return chain.filter(exchange.mutate().request(serverHttpRequest).build());
            }));
        });
    }

    private String getRequestIdFromPayload(String payload) {
        try {
            return objectMapper.readValue(payload, Map.class)
                .get("requestId").toString();
        } catch (Exception ignored) {
            log.warn("Could not read payload", ignored);
            return null;
        }
    }
}
