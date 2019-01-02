package com.learning.springboot.config.jwt;

import com.learning.springboot.config.GlobalConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Filters incoming requests and installs a Spring Security principal if a header corresponding to a valid user is
 * found.
 */
public class JwtAuthenticationConverter implements ServerAuthenticationConverter {
    private final TokenProvider tokenProvider;

    public JwtAuthenticationConverter(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    private Mono<String> resolveToken(ServerWebExchange exchange) {
        String jwt = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.isBlank(jwt)) {
            jwt = exchange.getSession().block().getAttribute(GlobalConstants.JWT.toString());
        }

        if (StringUtils.isBlank(jwt) || !jwt.startsWith("Bearer ")) {
            return Mono.empty();
        }

        return Mono.just(jwt.substring(7));
    }

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        return resolveToken(exchange)
                .filter(tokenProvider::validateToken)
                .map(tokenProvider::getAuthentication);
    }

}
