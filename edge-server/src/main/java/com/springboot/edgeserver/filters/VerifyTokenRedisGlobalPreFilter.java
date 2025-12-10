package com.springboot.edgeserver.filters;

import java.util.List;
import java.util.Map;

import io.micrometer.tracing.BaggageInScope;
import io.micrometer.tracing.Tracer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.session.web.server.session.SpringSessionWebSessionStore;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR;

/**
 * Verify if the bearer token still exists on Redis before processing the request.
 */
@Slf4j
@AllArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class VerifyTokenRedisGlobalPreFilter implements GlobalFilter {
    private final SpringSessionWebSessionStore springSessionWebSessionStore;

    private final Tracer tracer;

    private final JsonMapper jsonMapper;

    private final GatewayProperties gatewayProperties;

    private final List<HttpMessageReader<?>> messageReaders = HandlerStrategies.withDefaults().messageReaders();

    private final String REQUEST_ID_HEADER = "requestId";

    private final String[] skipPaths = {"/admin", "/api/logout", "/login/oauth2/", "/oauth2/",
            "/api/authenticate", "/api/authenticatedUser", "/oauth/", "/swagger/", "/swagger-ui/", "/.well-known",
            "/v3/api-docs", "/public/build/", "/api/csrf", "/login", "/default-ui.css", "/webauthn", "/ott"};


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        RequestPath path = request.getPath();
        log.info("VerifyTokenRedisGlobalPreFilter:filter:gatewayProperties:path {}", path.value());
        if (StringUtils.startsWithAny(path.value(), skipPaths)) {
            log.debug("Skip token redis validation for following path: {}", path);
            String authorizationHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (StringUtils.isBlank(authorizationHeader)) {
                log.debug("Trying to set authorization header if user is authenticated.");
                return ReactiveSecurityContextHolder.getContext()
                        .flatMap(securityContext -> {
                            Authentication authentication = securityContext.getAuthentication();
                            if (authentication != null && authentication.isAuthenticated()) {
                                log.debug("User is authenticated:");
                                return exchange.getSession()
                                        .flatMap(session -> springSessionWebSessionStore.retrieveSession(session.getId())
                                                .flatMap(sessionObj -> {
                                                    WebSession sessionRepository = (WebSession) sessionObj;
                                                    OAuth2AccessToken accessToken = sessionRepository.getAttribute("token");
                                                    log.debug("verifyTokenRedis:Set authorization header from redis session");

                                                    HttpHeaders writeableHeaders = HttpHeaders.readOnlyHttpHeaders(
                                                            exchange.getRequest().getHeaders());
                                                    ServerHttpRequestDecorator writeableRequest = new ServerHttpRequestDecorator(
                                                            exchange.getRequest()) {
                                                        @Override
                                                        public HttpHeaders getHeaders() {
                                                            return writeableHeaders;
                                                        }
                                                    };
                                                    //TODO Cannot add new header for the moment - https://github.com/spring-projects/spring-security/issues/15989#issuecomment-2442660753
                                                    writeableRequest.getHeaders().add(HttpHeaders.AUTHORIZATION, String.format("%s %s", accessToken.getTokenType().getValue(), accessToken.getTokenValue()));
                                                    ServerWebExchange writeableExchange = exchange.mutate()
                                                            .request(writeableRequest)
                                                            .build();

                                                    return chain.filter(writeableExchange);
                                                }));
                            }
                            return chain.filter(exchange);
                        }).switchIfEmpty(chain.filter(exchange));
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
        /*OAuth2Authentication oAuth2Authentication = tokenStore.readAuthentication(authorizationHeader.replaceFirst("(?i)Bearer ", ""));
        if (oAuth2Authentication == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token not found in redis."));
        }*/
        log.debug("Found valid redis for a given token");
        return chain.filter(exchange);
        /*if (request.getHeaders().containsKey(REQUEST_ID_HEADER) || request.getMethod() == HttpMethod.GET || request.getMethod() == HttpMethod.HEAD) {
            return chain.filter(exchange);
        } else {
            return addRequestIdHeader(chain, exchange);
        }*/
    }

    //TODO Cannot add new header for the moment - https://github.com/spring-projects/spring-security/issues/15989#issuecomment-2442660753
    @Deprecated
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
                        try (BaggageInScope baggage = tracer.createBaggageInScope(REQUEST_ID_HEADER, requestId)) {
                            serverHttpRequest.mutate().header(REQUEST_ID_HEADER, baggage.get());
                        }
                    } else {
                        log.warn("Not found requestId field in payload");
                    }
                }
                exchange.getAttributes().remove(CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR);
                return chain.filter(exchange.mutate().request(serverHttpRequest).build());
            }));
        });
    }

    private String getRequestIdFromPayload(String payload) {
        try {
            Map map = jsonMapper.readValue(payload, Map.class);
            if (!map.containsKey(REQUEST_ID_HEADER)) {
                return null;
            }
            return map.get(REQUEST_ID_HEADER).toString();
        } catch (Exception ignored) {
            log.warn("Could not read payload", ignored);
            return null;
        }
    }
}
