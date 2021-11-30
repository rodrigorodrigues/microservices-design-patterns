package com.microservice.authentication.controller;

import java.util.Set;
import java.util.stream.Collectors;

import com.microservice.authentication.common.model.Authority;
import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/account")
@AllArgsConstructor
public class AccountController {
    private final AuthenticationCommonRepository authenticationCommonRepository;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public UserDto index(Authentication auth) {
        com.microservice.authentication.common.model.Authentication authentication = authenticationCommonRepository.findByEmail(auth.getName());
        UserDto userDto = new UserDto(authentication.getUsername(), authentication.getFullName(), authentication.getEmail(), authentication.getAuthorities().stream()
                .map(Authority::getAuthority)
                .collect(Collectors.toSet()));
        log.debug("AccountController:userDto: {}", userDto);
        return userDto;
    }

    public static record UserDto(String login, String langKey, String fullName, String email, boolean activated, Set<String> authorities) {
        public UserDto(String login, String fullName, String email, Set<String> authorities) {
            this(login, "en", fullName, email, true, authorities);
        }
    }
}
