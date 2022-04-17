package com.microservice.authentication.controller;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.microservice.authentication.service.RedisTokenStoreService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/account")
@AllArgsConstructor
public class AccountController {
    private final RedisTokenStoreService redisTokenStoreService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public UserDto index(Authentication authentication) {
        OAuth2AccessToken token = redisTokenStoreService.getToken(authentication);
        Map<String, Object> additionalInformation = token.getAdditionalInformation();
        HashSet<String> authorities = new HashSet<>(Arrays.asList(additionalInformation.get("auth")
            .toString().split(",")));
        UserDto userDto = new UserDto(additionalInformation.get("name").toString(), additionalInformation.get("fullName").toString(), additionalInformation.get("sub").toString(), authorities);
        log.debug("AccountController:userDto: {}", userDto);
        return userDto;
    }

    public static record UserDto(String login, String langKey, String fullName, String email, boolean activated, Set<String> authorities) {
        public UserDto(String login, String fullName, String email, Set<String> authorities) {
            this(login, "en", fullName, email, true, authorities);
        }
    }
}
