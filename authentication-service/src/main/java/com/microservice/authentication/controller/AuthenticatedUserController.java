package com.microservice.authentication.controller;

import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import com.microservice.authentication.dto.JwtTokenDto;
import com.microservice.authentication.service.RedisTokenStoreService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Controller for authenticated user.
 */
@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/authenticatedUser")
public class AuthenticatedUserController {

    private final AuthenticationCommonRepository authenticationCommonRepository;

    private final RedisTokenStoreService redisTokenStoreService;

    @GetMapping
    public ResponseEntity<JwtTokenDto> authenticatedUser(@AuthenticationPrincipal Authentication authentication, HttpServletRequest request) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        OAuth2Authentication oAuth2Authentication;
        OAuth2Request oAuth2Request;
        if (authentication instanceof OAuth2AuthenticationToken) {
            DefaultOidcUser oidcUser = (DefaultOidcUser) authentication.getPrincipal();
            Map attributes = oidcUser.getAttributes();
            com.microservice.authentication.common.model.Authentication findUserByEmail = authenticationCommonRepository.findByEmail(oidcUser.getEmail());
            Set<String> scopes = (findUserByEmail != null ? findUserByEmail.getScopes() : new HashSet<>());
            oAuth2Request = new OAuth2Request(attributes, authentication.getName(), authentication.getAuthorities(),
                true, scopes, null, null, null, null);
            oAuth2Authentication = new OAuth2Authentication(oAuth2Request, authentication);
        } else {
            oAuth2Request = new OAuth2Request(null, authentication.getName(), authentication.getAuthorities(),
                true, Collections.singleton("read"), null, null, null, null);
            oAuth2Authentication = new OAuth2Authentication(oAuth2Request, authentication);
            httpHeaders.add("sessionId", request.getSession().getId());
        }
        OAuth2AccessToken token = redisTokenStoreService.generateToken(authentication, oAuth2Authentication);
        httpHeaders.add(HttpHeaders.AUTHORIZATION, String.format("%s %s", token.getTokenType(), token.getValue()));
        JwtTokenDto body = new JwtTokenDto(httpHeaders.getFirst(HttpHeaders.AUTHORIZATION));
        log.debug("AuthenticatedUserController:body: {}", body);
        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(httpHeaders)
            .body(body);
    }
}
