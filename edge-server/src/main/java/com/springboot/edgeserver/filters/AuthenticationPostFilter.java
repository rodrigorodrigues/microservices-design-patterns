package com.springboot.edgeserver.filters;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;

import static org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository.DEFAULT_SPRING_SECURITY_CONTEXT_ATTR_NAME;

/**
 * Filter applied after authentication to set user in the session.
 */
@Slf4j
@AllArgsConstructor
@Component
public class AuthenticationPostFilter implements GatewayFilter {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final TokenStore tokenStore;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
                .then(Mono.just(exchange))
                .filter(w -> w.getResponse().getStatusCode() == HttpStatus.OK)
                .flatMap(w -> {
                    log.debug("Set token to edge");
                    String authorizationHeader;
                    ServerHttpResponse response = w.getResponse();
                    if (response.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                        log.debug("Found authorization Header");
                        authorizationHeader = response.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                    }
                    else {
                        DataBufferFactory bufferFactory = response.bufferFactory();
                        String json = bufferFactory.allocateBuffer().toString(StandardCharsets.UTF_8);
                        try {
                            log.debug("Trying to get token from response");
                            authorizationHeader = (String) objectMapper.readValue(json, Map.class).get("access_token");
                        }
                        catch (JsonProcessingException e) {
                            log.error("Could not find a valid token", e);
                            return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Could not find a valid token", e));
                        }
                    }
                    OAuth2Authentication authentication = tokenStore.readAuthentication(authorizationHeader.replaceFirst("(?i)Bearer ", ""));
                    if (authentication == null || !authentication.isAuthenticated()) {
                        return Mono.error(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated in redis"));
                    }
                    return w.getSession().map(s -> {
                        SecurityContextImpl securityContext = new SecurityContextImpl(authentication);
                        s.getAttributes().put(DEFAULT_SPRING_SECURITY_CONTEXT_ATTR_NAME, securityContext);
                        log.debug("filter:Authenticated user in the session: {}", authentication.getName());
                        return s;
                    });
                })
                .flatMap(WebSession::save)
                .then();
    }
}
