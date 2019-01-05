package com.learning.springboot.config.jwt;

import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import reactor.core.publisher.Mono;

/**
 * Validate if user is already authenticated.
 */
public class CustomReactiveAuthenticationManager extends UserDetailsRepositoryReactiveAuthenticationManager {
    public CustomReactiveAuthenticationManager(ReactiveUserDetailsService userDetailsService) {
        super(userDetailsService);
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        if (authentication.isAuthenticated()) {
            return Mono.just(authentication);
        }
        return super.authenticate(authentication);
    }
}
