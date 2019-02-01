package com.learning.springboot.config.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import reactor.core.publisher.Mono;

import java.util.function.Predicate;

/**
 * Validate if user is already authenticated.
 */
@Slf4j
public class CustomReactiveAuthenticationManager extends UserDetailsRepositoryReactiveAuthenticationManager {
    private final ReactiveUserDetailsService userDetailsService;

    public CustomReactiveAuthenticationManager(ReactiveUserDetailsService userDetailsService) {
        super(userDetailsService);
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        log.debug("CustomReactiveAuthenticationManager:authentication: {}", authentication);
        return userDetailsService.findByUsername(authentication.getName())
            .filter(isUserEnabled())
            .switchIfEmpty(Mono.error(new LockedException(String.format("User(%s) is locked", authentication.getName()))))
            .flatMap(u -> super.authenticate(authentication));
    }

    private Predicate<UserDetails> isUserEnabled() {
        return u -> u.isEnabled() && u.isAccountNonExpired() && u.isAccountNonLocked() && u.isCredentialsNonExpired();
    }
}
