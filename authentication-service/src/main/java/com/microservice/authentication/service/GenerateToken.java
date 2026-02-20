package com.microservice.authentication.service;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.microservice.authentication.autoconfigure.AuthenticationProperties;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

@Component
public class GenerateToken {
    private final JwtEncoder jwtEncoder;

    private final AuthenticationProperties properties;

    private final OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer;

    public GenerateToken(JwtEncoder jwtEncoder, AuthenticationProperties properties, OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.jwtTokenCustomizer = jwtTokenCustomizer;
    }

    public OAuth2AccessToken generateToken(Authentication authentication) {
        Instant now = Instant.now();
        long expiry = 36000L;
        Set<String> scopes = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());

        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
            .issuer(properties.getIssuer())
            .issuedAt(now)
            .expiresAt(now.plusSeconds(expiry))
            .subject(authentication.getName())
            .claim("scopes", scopes)
            .claim("authorities", scopes);

        // Apply customizer if available
        if (jwtTokenCustomizer != null) {
            JwsHeader.Builder headersBuilder = JwsHeader.with(SignatureAlgorithm.RS256);
            JwtEncodingContext.Builder contextBuilder = JwtEncodingContext.with(headersBuilder, claimsBuilder)
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .principal(authentication);
            JwtEncodingContext context = contextBuilder.build();
            jwtTokenCustomizer.customize(context);
        }

        JwtClaimsSet claims = claimsBuilder.build();
        Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(claims));
        return new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
            jwt.getTokenValue(),
            jwt.getIssuedAt(),
            jwt.getExpiresAt(),
            new HashSet<>(jwt.getClaimAsStringList("scopes")));
    }
}
