package com.learning.springboot.config;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.ELRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final RequestMatcher requestHasContentTypeHeader = new ELRequestMatcher("hasHeader('Content-Type','application/json')");

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        if (requestHasContentTypeHeader.matches(request)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ExceptionUtils.getMessage(authException));
        } else {
            new LoginUrlAuthenticationEntryPoint("/login").commence(request, response, authException);
        }
    }
}
