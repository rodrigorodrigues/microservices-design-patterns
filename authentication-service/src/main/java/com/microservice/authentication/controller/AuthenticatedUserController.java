package com.microservice.authentication.controller;

import com.microservice.authentication.common.service.SharedAuthenticationService;
import com.microservice.authentication.dto.JwtTokenDto;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
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

    private final JwtAccessTokenConverter jwtAccessTokenConverter;

    private final SharedAuthenticationService sharedAuthenticationService;

    @GetMapping
    public ResponseEntity<JwtTokenDto> authenticatedUser(@AuthenticationPrincipal Authentication authentication) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        if (authentication instanceof OAuth2Authentication) {
            OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails) authentication.getDetails();
            httpHeaders.add(HttpHeaders.AUTHORIZATION, String.format("%s %s", details.getTokenType(), details.getTokenValue()));
        } else {
            OAuth2Request oAuth2RequestRequest= new OAuth2Request(null, "client", null, true, null,
                null, null, null, null);

            OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(oAuth2RequestRequest, authentication);

            Map<String, String> map = new HashMap<>();
            map.put(DefaultOAuth2AccessToken.ACCESS_TOKEN, UUID.randomUUID().toString());
            map.put(DefaultOAuth2AccessToken.TOKEN_TYPE, DefaultOAuth2AccessToken.BEARER_TYPE);
            map.put(DefaultOAuth2AccessToken.EXPIRES_IN, "30");
            map.put(DefaultOAuth2AccessToken.SCOPE, "read");
            OAuth2AccessToken accessToken = DefaultOAuth2AccessToken.valueOf(map);

            OAuth2AccessToken oAuth2AccessToken = jwtAccessTokenConverter.enhance(accessToken, oAuth2Authentication);

            httpHeaders.add(HttpHeaders.AUTHORIZATION, String.format("%s %s", oAuth2AccessToken.getTokenType(), oAuth2AccessToken.getValue()));
        }

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(httpHeaders)
            .body(new JwtTokenDto(httpHeaders.getFirst(HttpHeaders.AUTHORIZATION)));
    }

    private String getFullName(Authentication authentication) {
        return ((com.microservice.authentication.common.model.Authentication) sharedAuthenticationService.loadUserByUsername(authentication.getName())).getFullName();
    }
}
