package com.learning.springboot.service;

import com.learning.springboot.model.Authentication;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import reactor.core.publisher.Mono;

public interface AuthenticationService extends ReactiveUserDetailsService {
    Mono<Authentication> findByEmail(String email);
}
