package com.microservice.user.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Class for auditing mongo entities changes.
 */
@Component
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .map(a -> {
                if (a instanceof OAuth2AuthenticationToken) {
                    DefaultOidcUser oidcUser = (DefaultOidcUser) a.getPrincipal();
                    return oidcUser.getEmail();
                } else {
                    return a.getName();
                }
            });
    }
}
