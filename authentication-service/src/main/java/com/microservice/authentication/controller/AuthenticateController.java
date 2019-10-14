package com.microservice.authentication.controller;

import com.microservice.authentication.common.model.Authentication;
import com.microservice.authentication.dto.JwtTokenDto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import springfox.documentation.annotations.ApiIgnore;

import java.util.Objects;

/**
 * Controller for user authentication.
 */
@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/authenticate")
public class AuthenticateController {

    @PostMapping
    public Mono<ResponseEntity<JwtTokenDto>> authenticate(@AuthenticationPrincipal Authentication authentication, @ApiIgnore ServerHttpResponse response) {
        log.debug("AuthenticateController:response: {}", ReflectionToStringBuilder.toString(response));
        log.debug("AuthenticateController:authentication: {}", ReflectionToStringBuilder.toString(authentication));

        return Mono.just(ResponseEntity
                .status(Objects.requireNonNull(response.getStatusCode()))
                .headers(response.getHeaders())
                .body(new JwtTokenDto(response.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))));
    }

}
