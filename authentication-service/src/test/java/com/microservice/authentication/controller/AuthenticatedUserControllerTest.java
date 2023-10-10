package com.microservice.authentication.controller;

import com.microservice.authentication.service.RedisOauth2TokenStoreServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthenticatedUserControllerTest {

    @Mock
    RedisOauth2TokenStoreServiceImpl redisTokenStoreService;

    AuthenticatedUserController authenticatedUserController;

    @BeforeEach
    public void setup() {
        authenticatedUserController = new AuthenticatedUserController(redisTokenStoreService);
    }

    @Test
    void testAuthenticatedUser() {
        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        when(accessToken.getTokenType()).thenReturn("Bearer");
        when(accessToken.getValue()).thenReturn("Mock JWT");
        when(redisTokenStoreService.getToken(any(Authentication.class))).thenReturn(accessToken);

        ResponseEntity<OAuth2AccessToken> jwtTokenDtoResponseEntity = authenticatedUserController.authenticatedUser(new UsernamePasswordAuthenticationToken("user", "password"));

        assertThat(jwtTokenDtoResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(jwtTokenDtoResponseEntity.getHeaders()).containsKeys(HttpHeaders.CONTENT_TYPE, HttpHeaders.AUTHORIZATION);
        assertThat(jwtTokenDtoResponseEntity.getBody()).isNotNull();
    }

    @Test
    void testAuthenticatedUserWithOauth2() {
        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        when(accessToken.getTokenType()).thenReturn("Bearer");
        when(accessToken.getValue()).thenReturn("Mock JWT");
        when(redisTokenStoreService.getToken(any(Authentication.class))).thenReturn(accessToken);

        OAuth2AuthenticationToken oAuth2AuthenticationToken = mock(OAuth2AuthenticationToken.class);
        OidcIdToken oidcIdToken = OidcIdToken.withTokenValue("Test").claim("sub", "name").build();
        DefaultOidcUser oidcUser = new DefaultOidcUser(Collections.singletonList(new SimpleGrantedAuthority("ADMIN")), oidcIdToken);
        when(oAuth2AuthenticationToken.getPrincipal()).thenReturn(oidcUser);

        ResponseEntity<OAuth2AccessToken> jwtTokenDtoResponseEntity = authenticatedUserController.authenticatedUser(oAuth2AuthenticationToken);

        assertThat(jwtTokenDtoResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(jwtTokenDtoResponseEntity.getHeaders()).containsKeys(HttpHeaders.CONTENT_TYPE, HttpHeaders.AUTHORIZATION);
        assertThat(jwtTokenDtoResponseEntity.getBody()).isNotNull();
    }
}
