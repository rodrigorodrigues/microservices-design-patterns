package com.microservice.authentication.common.service;

import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import reactor.core.publisher.Mono;

/**
 * Reactive version of {@link org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider}
 *
 * This manager receives a {@link PreAuthenticatedAuthenticationToken}, checks that associated account is not disabled,
 * expired, or blocked, and returns new authenticated {@link PreAuthenticatedAuthenticationToken}.
 *
 * If no {@link UserDetailsChecker} is provided, a default {@link AccountStatusUserDetailsChecker} will be
 * created.
 *
 * @author Alexey Nesterov
 * @since 5.2
 */
public class ReactivePreAuthenticatedAuthenticationManager
    implements ReactiveAuthenticationManager {

    private final ReactiveUserDetailsService userDetailsService;
    private final UserDetailsChecker userDetailsChecker;

    public ReactivePreAuthenticatedAuthenticationManager(ReactiveUserDetailsService userDetailsService) {
        this(userDetailsService, new AccountStatusUserDetailsChecker());
    }

    public ReactivePreAuthenticatedAuthenticationManager(
        ReactiveUserDetailsService userDetailsService,
        UserDetailsChecker userDetailsChecker) {
        this.userDetailsService = userDetailsService;
        this.userDetailsChecker = userDetailsChecker;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        return Mono.just(authentication)
            .map(Authentication::getName)
            .flatMap(userDetailsService::findByUsername)
            .switchIfEmpty(Mono.error(() -> new UsernameNotFoundException(String.format("User(%s) not found", authentication.getName()))))
            .doOnNext(userDetailsChecker::check)
            .map(ud -> authentication);
    }

}
