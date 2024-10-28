package com.microservice.authentication.controller;

import java.util.Collections;

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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthenticatedUserControllerTest {

    @Mock
    SessionRepository sessionRepository;;

    AuthenticatedUserController authenticatedUserController;

    @BeforeEach
    public void setup() {
        authenticatedUserController = new AuthenticatedUserController(sessionRepository);
    }

    @Test
    void testAuthenticatedUser() {
        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        when(accessToken.getTokenType()).thenReturn(OAuth2AccessToken.TokenType.BEARER);
        when(accessToken.getTokenValue()).thenReturn("Mock JWT");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession mockSession = new MockHttpSession();
        request.setSession(mockSession);
        Session session = mock(Session.class);
        when(sessionRepository.findById(anyString())).thenReturn(session);
        when(session.getAttribute(anyString())).thenReturn(accessToken);

        ResponseEntity<OAuth2AccessToken> jwtTokenDtoResponseEntity = authenticatedUserController.authenticatedUser(new UsernamePasswordAuthenticationToken("user", "password"), request);

        assertThat(jwtTokenDtoResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(jwtTokenDtoResponseEntity.getHeaders()).containsKeys(HttpHeaders.CONTENT_TYPE, HttpHeaders.AUTHORIZATION);
        assertThat(jwtTokenDtoResponseEntity.getBody()).isNotNull();
    }

    @Test
    void testAuthenticatedUserWithOauth2() {
        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        when(accessToken.getTokenType()).thenReturn(OAuth2AccessToken.TokenType.BEARER);
        when(accessToken.getTokenValue()).thenReturn("Mock JWT");
        //when(redisTokenStoreService.getToken(any(Authentication.class), anyBoolean())).thenReturn(accessToken);

        OAuth2AuthenticationToken oAuth2AuthenticationToken = mock(OAuth2AuthenticationToken.class);
        OidcIdToken oidcIdToken = OidcIdToken.withTokenValue("Test").claim("sub", "name").build();
        DefaultOidcUser oidcUser = new DefaultOidcUser(Collections.singletonList(new SimpleGrantedAuthority("ADMIN")), oidcIdToken);
        when(oAuth2AuthenticationToken.getPrincipal()).thenReturn(oidcUser);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession mockSession = new MockHttpSession();
        request.setSession(mockSession);
        Session session = mock(Session.class);
        when(sessionRepository.findById(anyString())).thenReturn(session);
        when(session.getAttribute(anyString())).thenReturn(accessToken);

        ResponseEntity<OAuth2AccessToken> jwtTokenDtoResponseEntity = authenticatedUserController.authenticatedUser(oAuth2AuthenticationToken, request);

        assertThat(jwtTokenDtoResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(jwtTokenDtoResponseEntity.getHeaders()).containsKeys(HttpHeaders.CONTENT_TYPE, HttpHeaders.AUTHORIZATION);
        assertThat(jwtTokenDtoResponseEntity.getBody()).isNotNull();
    }
}
