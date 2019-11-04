package com.microservice.authentication.common.service;

import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import reactor.core.publisher.Mono;

public interface SharedAuthenticationService extends UserDetailsService, ReactiveUserDetailsService {
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;

    Mono<UserDetails> findByUsername(String username);
}
