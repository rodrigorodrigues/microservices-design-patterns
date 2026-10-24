package com.microservice.authentication.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import org.springframework.security.oauth2.core.OAuth2AccessToken;

/**
 * Response shape for token-issuing endpoints (login success, /api/authenticatedUser,
 * /api/refreshToken). Carries both the Spring-style fields ({@code tokenValue}) the
 * frontend already reads to detect success, and the standard OAuth2 token-response
 * fields ({@code expires_in}, {@code refresh_token}) the react-webapp's refresh-timer
 * logic (App.js) expects - without these, the frontend can never compute a real
 * expiry and never has a refresh token to send back.
 */
public record TokenResponse(
    String tokenValue,
    String tokenType,
    Instant issuedAt,
    Instant expiresAt,
    Set<String> scopes,
    long expires_in,
    String refresh_token
) {
    public static TokenResponse from(OAuth2AccessToken token, String refreshToken) {
        long expiresIn = token.getExpiresAt() != null
            ? Math.max(0, Duration.between(Instant.now(), token.getExpiresAt()).getSeconds())
            : 0;
        return new TokenResponse(
            token.getTokenValue(),
            token.getTokenType().getValue(),
            token.getIssuedAt(),
            token.getExpiresAt(),
            token.getScopes(),
            expiresIn,
            refreshToken
        );
    }
}
