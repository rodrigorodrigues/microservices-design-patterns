package com.springboot.edgeserver.config;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.stream.Stream;

import static com.netflix.zuul.context.RequestContext.getCurrentContext;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_DECORATION_FILTER_ORDER;

@Slf4j
@Component
public class AuthenticationZuulFilter extends ZuulFilter {

    private final String[] paths = {"/zipkin", "/grafana"};

    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return PRE_DECORATION_FILTER_ORDER - 1;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext context = getCurrentContext();
        String servletPath = context.getRequest().getServletPath();
        return Stream.of(paths).anyMatch(p -> servletPath.startsWith(p) && !servletPath.contains("/public/"));
    }

    @Override
    public Object run() {
        RequestContext context = getCurrentContext();
        HttpServletRequest request = context.getRequest();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.debug("authenticationZuulFilter: {}", authentication);
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("User is not authenticated: {}", authentication.getName());
            context.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value());
            context.setResponseBody(String.format("To access(%s) user must be authenticated!", request.getRequestURI()));
        } else if (authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch("ADMIN"::equals)) {
            log.debug("User has admin role: {}", authentication.getAuthorities());
            context.addZuulRequestHeader("X-WEBAUTH-USER", "admin");
        } else {
            log.debug("User has not ROLE_ADMIN: {}", authentication.getName());
            context.setResponseStatusCode(HttpStatus.FORBIDDEN.value());
        }
        return null;
    }

}
