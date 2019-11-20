package com.microservice.authentication.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.microservice.authentication.dto.JwtTokenDto;
import com.microservice.jwt.common.TokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;

@ExtendWith(MockitoExtension.class)
class AuthenticatedUserControllerTest {

    @Mock
    TokenProvider tokenProvider;

    AuthenticatedUserController authenticatedUserController;

    @BeforeEach
    public void setup() {
        authenticatedUserController = new AuthenticatedUserController(tokenProvider);
    }

    @Test
    void testAuthenticatedUser() {
        when(tokenProvider.createToken(any(), anyString(), anyBoolean())).thenReturn("Mock JWT");

        ResponseEntity<JwtTokenDto> jwtTokenDtoResponseEntity = authenticatedUserController.authenticatedUser(new UsernamePasswordAuthenticationToken("user", "password"));

        assertThat(jwtTokenDtoResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(jwtTokenDtoResponseEntity.getHeaders()).containsOnlyKeys(HttpHeaders.CONTENT_TYPE, HttpHeaders.AUTHORIZATION);
        assertThat(jwtTokenDtoResponseEntity.getBody()).isNotNull();
        assertThat(jwtTokenDtoResponseEntity.getBody().getIdToken()).isEqualTo("Bearer Mock JWT");
    }

    @Test
    void testAuthenticatedUserWithOauth2() {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken("user", "password");

        OAuth2Request oAuth2RequestRequest= new OAuth2Request(null, "client", null, true, null,
            null, null, null, null);

        OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(oAuth2RequestRequest, authentication);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(OAuth2AuthenticationDetails.ACCESS_TOKEN_TYPE, "bearer");
        request.setAttribute(OAuth2AuthenticationDetails.ACCESS_TOKEN_VALUE, "Mock JWT");
        OAuth2AuthenticationDetails accessToken = new OAuth2AuthenticationDetails(request);
        oAuth2Authentication.setDetails(accessToken);

        ResponseEntity<JwtTokenDto> jwtTokenDtoResponseEntity = authenticatedUserController.authenticatedUser(oAuth2Authentication);

        assertThat(jwtTokenDtoResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(jwtTokenDtoResponseEntity.getHeaders()).containsOnlyKeys(HttpHeaders.CONTENT_TYPE, HttpHeaders.AUTHORIZATION);
        assertThat(jwtTokenDtoResponseEntity.getBody()).isNotNull();
        assertThat(jwtTokenDtoResponseEntity.getBody().getIdToken()).isEqualTo("bearer Mock JWT");

        verifyZeroInteractions(tokenProvider);
    }
}
