package com.springboot.edgeserver.util;

import reactor.core.publisher.Mono;

import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

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
public class ReactivePreAuthenticatedAuthenticationManagerCustom
        implements ReactiveAuthenticationManager {

    private final ReactiveUserDetailsService userDetailsService;
    private final UserDetailsChecker userDetailsChecker;

    public ReactivePreAuthenticatedAuthenticationManagerCustom(ReactiveUserDetailsService userDetailsService) {
        this(userDetailsService, new AccountStatusUserDetailsChecker());
    }

    public ReactivePreAuthenticatedAuthenticationManagerCustom(
            ReactiveUserDetailsService userDetailsService,
            UserDetailsChecker userDetailsChecker) {
        this.userDetailsService = userDetailsService;
        this.userDetailsChecker = userDetailsChecker;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        return Mono.just(authentication).map(Authentication::getName)
                .flatMap(this.userDetailsService::findByUsername)
                .switchIfEmpty(Mono.error(() -> new UsernameNotFoundException("User not found")))
                .doOnNext(this.userDetailsChecker::check).map((userDetails) -> {
                    PreAuthenticatedAuthenticationToken result = new PreAuthenticatedAuthenticationToken(userDetails,
                            authentication.getCredentials(), userDetails.getAuthorities());
                    result.setDetails(authentication.getDetails());
                    return result;
                });
    }
}