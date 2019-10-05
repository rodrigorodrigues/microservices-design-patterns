package com.microservice.authentication.service;

import com.microservice.authentication.common.model.Authentication;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import reactor.core.publisher.Mono;

public interface AuthenticationService extends ReactiveUserDetailsService {
    Mono<Authentication> findByEmail(String email);
}
