package com.learning.springboot.config.jwt;

import com.learning.springboot.config.GlobalConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.learning.springboot.config.GlobalConstants.AUTHORIZATION_HEADER;

/**
 * Filters incoming requests and installs a Spring Security principal if a header corresponding to a valid user is
 * found.
 */
public class JWTFilter extends OncePerRequestFilter {
    private final String[] SHOULD_NOT_FILTER = {"/login", "/logout", "/error"};

    private final TokenProvider tokenProvider;

    public JWTFilter(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String jwt = resolveToken(request);
        SecurityContext securityContext = SecurityContextHolder.getContext();
        if (this.tokenProvider.validateToken(jwt)) {
            if (securityContext.getAuthentication() == null) {
                Authentication authentication = this.tokenProvider.getAuthentication(jwt);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } else {
            securityContext.setAuthentication(null);
            request = new ContentCachingRequestWrapper(request);
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        return StringUtils.containsAny(servletPath, SHOULD_NOT_FILTER);
    }

    private String resolveToken(HttpServletRequest request) {
        String jwt = request.getHeader(AUTHORIZATION_HEADER.toString());
        if (StringUtils.isBlank(jwt)) {
            jwt = (String) request.getSession().getAttribute(GlobalConstants.JWT.toString());
        }

        if (StringUtils.isBlank(jwt) || !jwt.startsWith("Bearer ")) {
            return null;
        }

        return jwt.substring(7);
    }
}
