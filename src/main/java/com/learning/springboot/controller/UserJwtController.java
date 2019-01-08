package com.learning.springboot.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.learning.springboot.config.jwt.TokenProvider;
import com.learning.springboot.dto.LoginDto;
import com.learning.springboot.model.Person;
import com.learning.springboot.service.PersonService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Controller for user authentication.
 */
@AllArgsConstructor
@RestController
@RequestMapping("/api/authenticate")
public class UserJwtController {

    private final TokenProvider tokenProvider;

    private final ReactiveAuthenticationManager authenticationManager;

    private final PersonService personService;

    @PostMapping
    public Mono<ResponseEntity<JwtToken>> authenticate(@RequestBody LoginDto loginDto) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword());

        return this.authenticationManager.authenticate(authenticationToken)
                .flatMap(a -> personService.findByUsername(loginDto.getUsername())
                        .map(u -> {
                            String jwt = "Bearer " + tokenProvider.createToken(a, loginDto.isRememberMe());
                            return ResponseEntity.ok().header(HttpHeaders.AUTHORIZATION, jwt)
                                    .body(new JwtToken(jwt, ((Person) u).getName()));
                        }));
    }

    @GetMapping
    public ResponseEntity<?> getUser(@AuthenticationPrincipal Authentication user) {
        if (user == null) {
            return ResponseEntity.ok("");
        } else {
            return ResponseEntity.ok(user);
        }
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

        private String name;
    }
}
