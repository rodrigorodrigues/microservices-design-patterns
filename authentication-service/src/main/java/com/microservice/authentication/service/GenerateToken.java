package com.microservice.authentication.service;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.microservice.authentication.autoconfigure.AuthenticationProperties;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
public class GenerateToken {
    private final JwtEncoder jwtEncoder;

    private final AuthenticationProperties properties;

    public GenerateToken(JwtEncoder jwtEncoder, AuthenticationProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    public OAuth2AccessToken generateToken(Authentication authentication) {
        Instant now = Instant.now();
        long expiry = 36000L;
        Set<String> scopes = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(properties.getIssuer())
            .issuedAt(now)
            .expiresAt(now.plusSeconds(expiry))
            .subject(authentication.getName())
            .claim("scopes", scopes)
            .claim("authorities", scopes)
            .build();
        Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(claims));
        return new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
            jwt.getTokenValue(),
            jwt.getIssuedAt(),
            jwt.getExpiresAt(),
            new HashSet<>(jwt.getClaimAsStringList("scopes")));
    }
}
