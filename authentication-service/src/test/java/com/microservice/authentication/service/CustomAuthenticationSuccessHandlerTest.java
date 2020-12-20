package com.microservice.authentication.service;

import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomAuthenticationSuccessHandlerTest {
    @Mock
    AuthenticationCommonRepository authenticationCommonRepository;

    @Mock
    RedisTokenStoreService redisTokenStoreService;

    @Test
    void testOnAuthenticationSuccess() throws Exception {
        when(authenticationCommonRepository.findByEmail(anyString()))
            .thenReturn(com.microservice.authentication.common.model.Authentication.builder()
                .scopes(new HashSet<>(Arrays.asList("read", "password")))
                .build());
        OAuth2AccessToken auth2AccessToken = mock(OAuth2AccessToken.class);
        when(auth2AccessToken.getTokenType()).thenReturn("Bearer");
        when(auth2AccessToken.getValue()).thenReturn("Mock JWT");
        when(redisTokenStoreService.generateToken(any(Authentication.class), any(OAuth2Authentication.class))).thenReturn(auth2AccessToken);

        CustomAuthenticationSuccessHandler handler = new CustomAuthenticationSuccessHandler(authenticationCommonRepository, redisTokenStoreService);

        OidcIdToken oidcIdToken = OidcIdToken.withTokenValue("token")
            .claim(StandardClaimNames.SUB, "test")
            .claim(StandardClaimNames.EMAIL, "test")
            .build();
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ADMIN"));
        DefaultOidcUser defaultOidcUser = new DefaultOidcUser(authorities, oidcIdToken);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(defaultOidcUser, "", authorities);

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        assertThat(response.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer Mock JWT");
    }

    @Test
    void testOnAuthenticationSuccessNotCommitted() throws Exception {
        CustomAuthenticationSuccessHandler handler = new CustomAuthenticationSuccessHandler(authenticationCommonRepository, redisTokenStoreService);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setCommitted(true);

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, null);

        verifyNoInteractions(authenticationCommonRepository, redisTokenStoreService);
    }
}
