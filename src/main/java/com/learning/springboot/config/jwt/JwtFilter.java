package com.learning.springboot.config.jwt;

import com.learning.springboot.config.GlobalConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static com.learning.springboot.config.GlobalConstants.AUTHORIZATION_HEADER;

/**
 * Filters incoming requests and installs a Spring Security principal if a header corresponding to a valid user is
 * found.
 */
public class JwtFilter extends GenericFilterBean {
    private final TokenProvider tokenProvider;

    public JwtFilter(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
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

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest servletRequest = (HttpServletRequest) request;
        String jwt = resolveToken(servletRequest);
        SecurityContext securityContext = SecurityContextHolder.getContext();
        if (this.tokenProvider.validateToken(jwt)) {
            if (securityContext.getAuthentication() == null) {
                Authentication authentication = this.tokenProvider.getAuthentication(jwt);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } else {
            securityContext.setAuthentication(null);
            request = new ContentCachingRequestWrapper(servletRequest);
        }
        chain.doFilter(request, response);
    }
}
