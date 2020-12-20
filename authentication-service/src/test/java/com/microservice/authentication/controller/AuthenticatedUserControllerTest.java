package com.microservice.authentication.controller;

import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import com.microservice.authentication.dto.JwtTokenDto;
import com.microservice.authentication.service.RedisTokenStoreService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import java.util.Collections;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthenticatedUserControllerTest {

    @Mock
    AuthenticationCommonRepository authenticationCommonRepository;

    @Mock
    RedisTokenStoreService redisTokenStoreService;

    AuthenticatedUserController authenticatedUserController;

    @BeforeEach
    public void setup() {
        authenticatedUserController = new AuthenticatedUserController(authenticationCommonRepository, redisTokenStoreService);
    }

    @Test
    void testAuthenticatedUser() {
        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        when(accessToken.getTokenType()).thenReturn("Bearer");
        when(accessToken.getValue()).thenReturn("Mock JWT");
        when(redisTokenStoreService.generateToken(any(Authentication.class), any(OAuth2Authentication.class))).thenReturn(accessToken);

        ResponseEntity<JwtTokenDto> jwtTokenDtoResponseEntity = authenticatedUserController.authenticatedUser(new UsernamePasswordAuthenticationToken("user", "password"), new MockHttpServletRequest());

        assertThat(jwtTokenDtoResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(jwtTokenDtoResponseEntity.getHeaders()).containsKeys(HttpHeaders.CONTENT_TYPE, HttpHeaders.AUTHORIZATION);
        assertThat(jwtTokenDtoResponseEntity.getBody()).isNotNull();
        assertThat(jwtTokenDtoResponseEntity.getBody().getIdToken()).isEqualTo("Bearer Mock JWT");

        verifyNoInteractions(authenticationCommonRepository);
    }

    @Test
    void testAuthenticatedUserWithOauth2() {
        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        when(accessToken.getTokenType()).thenReturn("Bearer");
        when(accessToken.getValue()).thenReturn("Mock JWT");
        when(redisTokenStoreService.generateToken(any(Authentication.class), any(OAuth2Authentication.class))).thenReturn(accessToken);
        com.microservice.authentication.common.model.Authentication authentication = new com.microservice.authentication.common.model.Authentication();
        authentication.setScopes(new HashSet<>());
        when(authenticationCommonRepository.findByEmail(anyString())).thenReturn(authentication);

        OAuth2AuthenticationToken oAuth2AuthenticationToken = mock(OAuth2AuthenticationToken.class);
        OidcIdToken oidcIdToken = OidcIdToken.withTokenValue("Test").claim("sub", "name").build();
        DefaultOidcUser oidcUser = new DefaultOidcUser(Collections.singletonList(new SimpleGrantedAuthority("ADMIN")), oidcIdToken);
        when(oAuth2AuthenticationToken.getPrincipal()).thenReturn(oidcUser);
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseEntity<JwtTokenDto> jwtTokenDtoResponseEntity = authenticatedUserController.authenticatedUser(oAuth2AuthenticationToken, request);

        assertThat(jwtTokenDtoResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(jwtTokenDtoResponseEntity.getHeaders()).containsKeys(HttpHeaders.CONTENT_TYPE, HttpHeaders.AUTHORIZATION);
        assertThat(jwtTokenDtoResponseEntity.getBody()).isNotNull();
        assertThat(jwtTokenDtoResponseEntity.getBody().getIdToken()).isEqualTo("Bearer Mock JWT");

        verify(authenticationCommonRepository).findByEmail(any());
    }
}
