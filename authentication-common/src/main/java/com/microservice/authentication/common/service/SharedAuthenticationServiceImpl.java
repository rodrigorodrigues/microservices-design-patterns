package com.microservice.authentication.common.service;

import com.microservice.authentication.common.model.Authentication;
import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

@Slf4j
@AllArgsConstructor
public class SharedAuthenticationServiceImpl implements UserDetailsService {
    private final AuthenticationCommonRepository authenticationCommonRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Searching username: {}", username);
        Optional<Authentication> authentication = authenticationCommonRepository.findByEmail(username);
        if (authentication.isEmpty()) {
            authentication = authenticationCommonRepository.findById(username);
        }
        return authentication
            .orElseThrow(() -> new UsernameNotFoundException(String.format("Authentication(%s) not found!", username)));
    }
}
