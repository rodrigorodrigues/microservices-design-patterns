package com.springboot.edgeserver.filters;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.session.Session;
import org.springframework.session.web.server.session.SpringSessionWebSessionStore;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;

/**
 * Filter applied after authentication to set user in the session.
 */
@Slf4j
@AllArgsConstructor
@Component
public class AuthenticationPostFilter implements GatewayFilter {
    private final SpringSessionWebSessionStore<Session> springSessionWebSessionStore;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
                .then(Mono.just(exchange))
                .filter(w -> w.getResponse().getStatusCode() == HttpStatus.OK)
                .flatMap(w -> w.getSession().flatMap(session -> {
                    String sessionId = exchange.getResponse().getHeaders().getFirst("sessionId");
                    log.debug("sessionId:current: {}", session.getId());
                    log.debug("sessionId:header: {}", sessionId);
                    log.debug("session attributes:before: {}", session.getAttributes().size());
                    return springSessionWebSessionStore.retrieveSession(sessionId)
                            .doOnSuccess(s -> session.getAttributes().putAll(s.getAttributes()));
                })
                .flatMap(session -> {
                    log.debug("session attributes:after: {}", session.getAttributes().size());
                    return session.save();
                })
                .then());
                /*.flatMap(w -> w.getSession().map(session -> {
                    String sessionId = exchange.getResponse().getHeaders().getFirst("sessionId");
                    log.debug("sessionId: {}", sessionId);

                    return sessionRepository.findById(sessionId)
                            .flatMap(sessionRepositoryById -> {
                                log.debug("attributes: {}", sessionRepositoryById.getAttributeNames());
                                Authentication authentication = sessionRepositoryById.getAttribute(DEFAULT_SPRING_SECURITY_CONTEXT_ATTR_NAME);
                                SecurityContextImpl securityContext = new SecurityContextImpl(authentication);
                                session.getAttributes().put(DEFAULT_SPRING_SECURITY_CONTEXT_ATTR_NAME, securityContext);
                                log.debug("filter:Authenticated user in the session: {}", authentication.getName());
                                return ses
                            });
                })*/
                /*.flatMap(w -> {
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
                    OAuth2AccessToken accessToken = w.getSession().getAttribute("token");
                    //OAuth2AuthenticationToken authentication = applicationContext.getBean(FindByIndexNameSessionRepository.class)
                            .findByPrincipalName(authorizationHeader.replaceFirst("(?i)Bearer ", ""));
                    if (authentication == null || !authentication.isAuthenticated()) {
                        return Mono.error(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated in redis"));
                    }
                    return w.getSession().map(s -> {
                        SecurityContextImpl securityContext = new SecurityContextImpl(authentication);
                        s.getAttributes().put(DEFAULT_SPRING_SECURITY_CONTEXT_ATTR_NAME, securityContext);
                        log.debug("filter:Authenticated user in the session: {}", authentication.getName());
                        return s;
                    });
                })*/
                //.flatMap(s -> ((WebSession) s).save())
    }
}
