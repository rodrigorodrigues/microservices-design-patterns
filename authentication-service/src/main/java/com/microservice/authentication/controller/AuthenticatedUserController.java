package com.microservice.authentication.controller;

import java.util.Map;

import com.microservice.authentication.service.RedisTokenStoreService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for authenticated user.
 */
@Slf4j
@AllArgsConstructor
@RestController
public class AuthenticatedUserController {

    private final RedisTokenStoreService redisTokenStoreService;

    @GetMapping("/api/authenticatedUser")
    public ResponseEntity<OAuth2AccessToken> authenticatedUser(Authentication authentication) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        OAuth2AccessToken token = redisTokenStoreService.getToken(authentication);
        httpHeaders.add(HttpHeaders.AUTHORIZATION, String.format("%s %s", token.getTokenType(), token.getValue()));
        log.debug("AuthenticatedUserController:token: {}", token);
        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(httpHeaders)
            .body(token);
    }

    @PostMapping("/api/refreshToken")
    public ResponseEntity<OAuth2AccessToken> refreshToken(@RequestParam Map<String, String> parameters,
        Authentication authentication) {
        String clientId = authentication.getName();
        parameters.put(HttpHeaders.AUTHORIZATION, ((Jwt) authentication.getPrincipal()).getTokenValue());
        TokenRequest tokenRequest = new TokenRequest(parameters, clientId, null, "refresh_token");
        OAuth2AccessToken token = redisTokenStoreService.refreshToken(tokenRequest);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.add(HttpHeaders.AUTHORIZATION, String.format("%s %s", token.getTokenType(), token.getValue()));

        log.debug("AuthenticatedUserController:refreshToken: {}", token);
        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(httpHeaders)
            .body(token);
    }
}
