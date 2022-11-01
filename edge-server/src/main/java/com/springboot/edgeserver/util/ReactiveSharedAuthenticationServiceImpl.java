package com.springboot.edgeserver.util;

import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import com.microservice.authentication.common.service.SharedAuthenticationServiceImpl;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;

@Slf4j
public class ReactiveSharedAuthenticationServiceImpl extends SharedAuthenticationServiceImpl implements ReactiveUserDetailsService {
    public ReactiveSharedAuthenticationServiceImpl(AuthenticationCommonRepository authenticationCommonRepository) {
        super(authenticationCommonRepository);
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        log.info("Trying to find username: {}", username);
        return Mono.just(loadUserByUsername(username));
    }
}
