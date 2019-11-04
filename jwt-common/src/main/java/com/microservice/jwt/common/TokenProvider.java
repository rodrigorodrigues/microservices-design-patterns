package com.microservice.jwt.common;

import com.microservice.jwt.common.config.Java8SpringConfigurationProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.security.Key;
import java.security.KeyStore;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generate and validate JWT.
 */
@Slf4j
public class TokenProvider {

    private static final String AUTHORITIES_KEY = "authorities";

    private static final String AUTH_KEY = "auth";

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
    public void init() throws Exception {
        Java8SpringConfigurationProperties.Jwt jwt = configurationProperties.getJwt();
        if (!StringUtils.isEmpty(jwt.getBase64Secret())) {
            log.debug("Using a Base64-encoded JWT secret key");
            byte[] keyBytes = Base64.getDecoder().decode(jwt.getBase64Secret());
            this.key = Keys.hmacShaKeyFor(keyBytes);
        } else {
            KeyStore store = KeyStore.getInstance("jks");
            store.load(new FileSystemResource(jwt.getKeystore()).getInputStream(), jwt.getKeystorePassword().toCharArray());
            this.key = store.getKey(jwt.getKeystoreAlias(), jwt.getKeystorePassword().toCharArray());
        }
        this.tokenValidityInMilliseconds =
            1000 * jwt.getTokenValidityInSeconds();
        this.tokenValidityInMillisecondsForRememberMe =
            1000 * jwt
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
        claims.put(AUTH_KEY, String.join(",", authorities));
        claims.put(NAME_KEY, fullName);

        JwtBuilder jwtBuilder = Jwts.builder()
            .setSubject(authentication.getName())
            .addClaims(claims);
        if (!StringUtils.isEmpty(configurationProperties.getJwt().getBase64Secret())) {
            jwtBuilder.signWith(key, SignatureAlgorithm.HS512);
        } else {
            jwtBuilder.signWith(key);
        }
        return jwtBuilder
            .setExpiration(validity)
            .compact();
    }

    /**
     * Convert JWT to Authentication.
     * @param token JWT
     * @return authentication
     */
    public PreAuthenticatedAuthenticationToken getAuthentication(String token) {
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

        return new PreAuthenticatedAuthenticationToken(principal, "", authorities);
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
            log.error("Invalid JWT signature.",e );
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token.", e);
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token.", e);
        } catch (IllegalArgumentException e) {
            log.error("JWT token compact of handler are invalid.", e);
        }
        return false;
    }
}
