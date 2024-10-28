package com.microservice.authentication.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/account")
@AllArgsConstructor
public class AccountController {
    private final SessionRepository sessionRepository;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public UserDto index(Authentication authentication, HttpServletRequest request) {
        boolean oauth2Login = authentication.getPrincipal() instanceof OidcUser;
        HttpSession httpSession = request.getSession(false);
        Session session;
        if (httpSession != null) {
            session = sessionRepository.findById(httpSession.getId());
        } else {
            session = sessionRepository.findById(request.getHeader("sessionId"));
        }
        OAuth2AccessToken accessToken = session.getAttribute("token");
        log.debug("accessToken: {}", accessToken);
        Map<String, Object> additionalInformation = new HashMap<>(); //token.getAdditionalInformation();
        HashSet<String> authorities = new HashSet<>(Arrays.asList(additionalInformation.get("auth")
            .toString().split(",")));
        String login = additionalInformation.get("name") != null ? additionalInformation.get("name").toString() : null;
        String fullName = additionalInformation.get("fullName") != null ? additionalInformation.get("fullName")
            .toString() : null;
        String email = additionalInformation.get("sub") != null ? additionalInformation.get("sub").toString() : null;
        UserDto userDto = new UserDto(login, fullName, email, authorities);
        log.debug("AccountController:userDto: {}", userDto);
        return userDto;
    }

    public record UserDto(String login, String langKey, String fullName, String email, boolean activated, Set<String> authorities) {
        public UserDto(String login, String fullName, String email, Set<String> authorities) {
            this(login, "en", fullName, email, true, authorities);
        }
    }
}
