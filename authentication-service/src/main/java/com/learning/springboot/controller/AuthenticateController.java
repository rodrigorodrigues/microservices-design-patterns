package com.learning.springboot.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.learning.springboot.config.jwt.TokenProvider;
import com.learning.springboot.dto.LoginDto;
import com.learning.springboot.service.AuthenticationService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Controller for user authentication.
 */
@AllArgsConstructor
@RestController
@RequestMapping("/api/authenticate")
public class AuthenticateController {

    private final TokenProvider tokenProvider;

    private final ReactiveAuthenticationManager authenticationManager;

    private final AuthenticationService authenticationService;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<JwtToken>> authenticate(@RequestBody LoginDto loginDto) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword());

        return this.authenticationManager.authenticate(authenticationToken)
                .flatMap(a -> authenticationService.findByEmail(loginDto.getUsername())
                .map(u -> {
                    String jwt = "Bearer " + tokenProvider.createToken(a, u.getFullName(), loginDto.isRememberMe());
                    return ResponseEntity.ok().header(HttpHeaders.AUTHORIZATION, jwt)
                            .body(new JwtToken(jwt));
                }));
    }

    /**
     * Object to return as body in JWT Authentication.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class JwtToken {
        @JsonProperty("id_token")
        private String idToken;
    }
}
