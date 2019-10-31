package com.microservice.authentication.controller;

import com.microservice.authentication.dto.JwtTokenDto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for authenticated user.
 */
@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/authenticatedUser")
public class AuthenticatedUserController {

    @GetMapping
    public ResponseEntity<JwtTokenDto> authenticatedUser(@AuthenticationPrincipal(errorOnInvalidType = true) OAuth2Authentication oAuth2Authentication) {
        OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails) oAuth2Authentication.getDetails();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.AUTHORIZATION, String.format("%s %s", details.getTokenType(), details.getTokenValue()));
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(httpHeaders)
            .body(new JwtTokenDto(httpHeaders.getFirst(HttpHeaders.AUTHORIZATION)));
    }
}
