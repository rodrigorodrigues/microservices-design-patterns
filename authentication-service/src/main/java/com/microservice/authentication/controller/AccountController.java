package com.microservice.authentication.controller;

import com.microservice.authentication.common.model.Authority;
import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/account")
@AllArgsConstructor
public class AccountController {
    private final AuthenticationCommonRepository authenticationCommonRepository;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public UserDto index(@AuthenticationPrincipal Authentication auth) {
        com.microservice.authentication.common.model.Authentication authentication = authenticationCommonRepository.findByEmail(auth.getName());
        UserDto userDto = new UserDto(authentication.getUsername(), authentication.getFullName(), authentication.getEmail(), authentication.getAuthorities().stream()
                .map(Authority::getAuthority)
                .collect(Collectors.toSet()));
        log.debug("AccountController:userDto: {}", userDto);
        return userDto;
    }

    @AllArgsConstructor
    @Getter
    public static class UserDto {
        private final String login;
        private final String langKey = "en";
        private final String fullName;
        private final String email;
        private final boolean activated = true;
        private final Set<String> authorities;
    }
}
