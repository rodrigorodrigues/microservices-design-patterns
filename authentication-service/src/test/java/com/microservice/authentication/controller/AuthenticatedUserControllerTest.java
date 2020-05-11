package com.microservice.authentication.controller;

import com.microservice.authentication.dto.JwtTokenDto;
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
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticatedUserControllerTest {

    @Mock
    DefaultTokenServices defaultTokenServices;

    AuthenticatedUserController authenticatedUserController;

    @BeforeEach
    public void setup() {
        authenticatedUserController = new AuthenticatedUserController(defaultTokenServices);
    }

    @Test
    void testAuthenticatedUser() {
        when(defaultTokenServices.createAccessToken(any())).thenReturn(new OAuth2AccessToken() {
            @Override
            public Map<String, Object> getAdditionalInformation() {
                return null;
            }

            @Override
            public Set<String> getScope() {
                return null;
            }

            @Override
            public OAuth2RefreshToken getRefreshToken() {
                return null;
            }

            @Override
            public String getTokenType() {
                return "Bearer";
            }

            @Override
            public boolean isExpired() {
                return false;
            }

            @Override
            public Date getExpiration() {
                return null;
            }

            @Override
            public int getExpiresIn() {
                return 0;
            }

            @Override
            public String getValue() {
                return "Mock JWT";
            }
        });

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

        verifyZeroInteractions(defaultTokenServices);
    }
}
