package com.microservice.authentication.common.service;

import com.microservice.authentication.common.model.Authentication;
import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Slf4j
@AllArgsConstructor
public class SharedAuthenticationServiceImpl implements SharedAuthenticationService {
    private final AuthenticationCommonRepository authenticationCommonRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Searching username: {}", username);
        Authentication authentication = Optional.ofNullable(authenticationCommonRepository.findByEmail(username))
            .orElseThrow(() -> new UsernameNotFoundException(String.format("Authentication(%s) not found!", username)));
        log.debug("Authentication: {}", authentication);
        return authentication;
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return Mono.just(loadUserByUsername(username));
    }
}
