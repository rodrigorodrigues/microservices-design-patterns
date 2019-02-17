package com.learning.springboot.service;

import com.learning.springboot.repository.AuthenticationRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class SharedAuthenticationServiceImpl implements UserDetailsService {
    private final AuthenticationRepository authenticationRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return Optional.ofNullable(authenticationRepository.findByEmail(username))
                .orElseThrow(() -> new UsernameNotFoundException(String.format("Authentication(%s) not found!", username)));
    }
}
