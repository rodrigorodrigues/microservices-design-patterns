package com.learning.springboot.config.jwt;

import com.learning.springboot.config.Java8SpringConfigurationProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generate and validate JWT.
 */
@Slf4j
@Component
public class TokenProvider {

    private static final String AUTHORITIES_KEY = "authorities";

    private static final String NAME_KEY = "name";

    private Key key;

    private long tokenValidityInMilliseconds;

    private long tokenValidityInMillisecondsForRememberMe;

    private final Java8SpringConfigurationProperties configurationProperties;

    public TokenProvider(Java8SpringConfigurationProperties configurationProperties) {
        this.configurationProperties = configurationProperties;
    }

    /**
     * Initiate jwt configuration.
     */
    @PostConstruct
    public void init() {
        log.debug("Using a Base64-encoded JWT secret key");
        byte[] keyBytes = Base64.getDecoder().decode(configurationProperties.getJwt().getBase64Secret());
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.tokenValidityInMilliseconds =
            1000 * configurationProperties.getJwt().getTokenValidityInSeconds();
        this.tokenValidityInMillisecondsForRememberMe =
            1000 * configurationProperties.getJwt()
                .getTokenValidityInSecondsForRememberMe();
    }

    /**
     * Create a new token for authenticated user.
     * @param authentication user
     * @param rememberMe boolean
     * @return jwt
     */
    public String createToken(Authentication authentication, String fullName, boolean rememberMe) {
        String[] authorities = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList())
            .toArray(new String[] {});

        long now = (new Date()).getTime();
        Date validity;
        if (rememberMe) {
            validity = new Date(now + this.tokenValidityInMillisecondsForRememberMe);
        } else {
            validity = new Date(now + this.tokenValidityInMilliseconds);
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put(AUTHORITIES_KEY, authorities);
        claims.put(NAME_KEY, fullName);

        return Jwts.builder()
            .setSubject(authentication.getName())
            .addClaims(claims)
            .signWith(key, SignatureAlgorithm.HS512)
            .setExpiration(validity)
            .compact();
    }

    /**
     * Convert JWT to Authentication.
     * @param token JWT
     * @return authentication
     */
    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(key)
            .parseClaimsJws(token)
            .getBody();

        List<String> authoritiesToken = claims.get(AUTHORITIES_KEY, List.class);
        Collection<? extends GrantedAuthority> authorities =
            authoritiesToken
                .stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        User principal = new User(claims.getSubject(), "", authorities);

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    /**
     * Check if token is valid.
     * @param authToken JWT
     * @return boolean
     */
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().setSigningKey(key).parseClaimsJws(authToken);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.info("Invalid JWT signature.");
            log.trace("Invalid JWT signature trace: {}", e);
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT token.");
            log.trace("Expired JWT token trace: {}", e);
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT token.");
            log.trace("Unsupported JWT token trace: {}", e);
        } catch (IllegalArgumentException e) {
            log.info("JWT token compact of handler are invalid.");
            log.trace("JWT token compact of handler are invalid trace: {}", e);
        }
        return false;
    }
}
