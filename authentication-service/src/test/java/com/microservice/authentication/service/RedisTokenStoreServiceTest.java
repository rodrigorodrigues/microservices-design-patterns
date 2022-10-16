package com.microservice.authentication.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisTokenStoreServiceTest {
    @Mock
    DefaultTokenServices defaultTokenServices;

    @Mock
    RedisTokenStore redisTokenStore;

    @Test
    void testGenerateToken() {
        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        when(defaultTokenServices.createAccessToken(any(OAuth2Authentication.class))).thenReturn(accessToken);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken("", "");

        OAuth2Request oAuth2Request = new OAuth2Request(null, authentication.getName(), authentication.getAuthorities(),
            true, null, null, null, null, null);
        OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(oAuth2Request, authentication);

        RedisTokenStoreService redisTokenStoreService = new RedisTokenStoreServiceImpl(defaultTokenServices, redisTokenStore);
        OAuth2AccessToken oAuth2AccessToken = redisTokenStoreService.generateToken(oAuth2Authentication);

        assertThat(oAuth2AccessToken).isNotNull();
    }

    @Test
    void testGetTokenFoundInStore() {
        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        when(accessToken.getAdditionalInformation()).thenReturn(Collections.singletonMap("jti", "mock"));

        when(redisTokenStore.findTokensByClientId(anyString())).thenReturn(Collections.singletonList(accessToken));

        RedisTokenStoreService redisTokenStoreService = new RedisTokenStoreServiceImpl(defaultTokenServices, redisTokenStore);
        OAuth2AccessToken oAuth2AccessToken = redisTokenStoreService.getToken(new UsernamePasswordAuthenticationToken("", ""));
        assertThat(oAuth2AccessToken).isNotNull();

        verifyNoInteractions(defaultTokenServices);
        verify(redisTokenStore, never()).storeAccessToken(any(OAuth2AccessToken.class), any(OAuth2Authentication.class));
    }

    @Test
    void testGetTokenGenerateNew() {
        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        when(defaultTokenServices.createAccessToken(any(OAuth2Authentication.class))).thenReturn(accessToken);

        RedisTokenStoreService redisTokenStoreService = new RedisTokenStoreServiceImpl(defaultTokenServices, redisTokenStore);
        OAuth2AccessToken oAuth2AccessToken = redisTokenStoreService.getToken(new UsernamePasswordAuthenticationToken("", ""));
        assertThat(oAuth2AccessToken).isNotNull();

        verify(defaultTokenServices).createAccessToken(any(OAuth2Authentication.class));
    }

    @Test
    void testRemoveAllTokensByAuthenticationUser() {
        when(redisTokenStore.findTokensByClientId(anyString())).thenReturn(Collections.singletonList(mock(OAuth2AccessToken.class)));

        OAuth2AuthenticationToken authenticationToken = mock(OAuth2AuthenticationToken.class);
        when(authenticationToken.getName()).thenReturn("test");

        RedisTokenStoreService redisTokenStoreService = new RedisTokenStoreServiceImpl(defaultTokenServices, redisTokenStore);
        redisTokenStoreService.removeAllTokensByAuthenticationUser(authenticationToken);

        verify(redisTokenStore).removeAccessToken(any(OAuth2AccessToken.class));
    }

    @Test
    void testRemoveAllTokensByAuthenticationUserWithNormalAuthentication() {
        RedisTokenStoreService redisTokenStoreService = new RedisTokenStoreServiceImpl(defaultTokenServices, redisTokenStore);

        redisTokenStoreService.removeAllTokensByAuthenticationUser(new UsernamePasswordAuthenticationToken("", ""));

        verifyNoInteractions(redisTokenStore);
    }
}
