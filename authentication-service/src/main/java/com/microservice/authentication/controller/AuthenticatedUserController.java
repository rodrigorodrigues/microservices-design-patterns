package com.microservice.authentication.controller;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for authenticated user.
 */
@Slf4j
@AllArgsConstructor
@RestController
public class AuthenticatedUserController {

    private final SessionRepository sessionRepository;

    @GetMapping("/api/authenticatedUser")
    public ResponseEntity<OAuth2AccessToken> authenticatedUser(Authentication authentication, HttpServletRequest request) {
        log.info("Generating token for user: {}", authentication.getName());
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        HttpSession httpSession = request.getSession(false);
        Session session;
        if (httpSession != null) {
            session = sessionRepository.findById(httpSession.getId());
        } else {
            session = sessionRepository.findById(request.getHeader("sessionId"));
        }
        OAuth2AccessToken accessToken = session.getAttribute("token");

        httpHeaders.add(HttpHeaders.AUTHORIZATION, String.format("%s %s", accessToken.getTokenType().getValue(), accessToken.getTokenValue()));
        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(httpHeaders)
            .body(accessToken);
    }

    @PostMapping("/api/refreshToken")
    public ResponseEntity<OAuth2AccessToken> refreshToken(@RequestParam Map<String, String> parameters,
        Authentication authentication) {
        String clientId = authentication.getName();
        parameters.put(HttpHeaders.AUTHORIZATION, ((Jwt) authentication.getPrincipal()).getTokenValue());
        //OAuth2UserRequest tokenRequest = new OAuth2UserRequest(parameters, clientId, null, "refresh_token");
        OAuth2AccessToken token = null;//oauth2TokenStoreService.refreshToken(null);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.add(HttpHeaders.AUTHORIZATION, String.format("%s %s", token.getTokenType().getValue(), token.getTokenValue()));

        log.debug("AuthenticatedUserController:refreshToken: {}", token);
        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(httpHeaders)
            .body(token);
    }
}
