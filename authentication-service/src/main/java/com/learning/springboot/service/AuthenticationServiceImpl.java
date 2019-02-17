package com.learning.springboot.service;

import com.learning.springboot.model.Authentication;
import com.learning.springboot.repository.AuthenticationRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final AuthenticationRepository authenticationRepository;

    @Override
    public Mono<Authentication> findByEmail(String email) {
        return authenticationRepository.findByEmail(email);
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) throws UsernameNotFoundException {
        return authenticationRepository.findByEmail(username)
            .switchIfEmpty(Mono.error(new UsernameNotFoundException(String.format("Authentication(%s) not found!", username))))
            .map(p -> p);
    }
}
