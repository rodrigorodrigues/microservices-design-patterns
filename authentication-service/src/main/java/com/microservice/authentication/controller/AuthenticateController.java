package com.microservice.authentication.controller;

import com.microservice.authentication.dto.JwtTokenDto;
import java.time.Instant;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.session.data.redis.ReactiveRedisOperationsSessionRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Controller for user authentication.
 */
@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/authenticate")
public class AuthenticateController {

    @PostMapping
    public Mono<ResponseEntity<JwtTokenDto>> authenticate(@ApiIgnore ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();

        return exchange.getSession()
            .flatMap(s -> {
                log.debug("AuthenticateController:sessionId: {}", s.getId());
                s.getAttributes().putIfAbsent(ReactiveRedisOperationsSessionRepository.DEFAULT_NAMESPACE+":lastAccessedTime", Instant
                    .now().toEpochMilli());
                s.getAttributes().forEach((k, v) -> log.debug("AuthenticateController:Key: {}\tValue: {}", k, v));
                return Mono.just(ResponseEntity
                    .status(Objects.requireNonNull(response.getStatusCode()))
                    .headers(response.getHeaders())
                    .body(new JwtTokenDto(response.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))));
            });
    }

}
