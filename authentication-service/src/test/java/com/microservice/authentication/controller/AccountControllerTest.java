package com.microservice.authentication.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.microservice.authentication.common.model.Authority;
import com.microservice.authentication.service.RedisOauth2TokenStoreServiceImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountControllerTest {

    @Test
    void testIndex() {
        RedisOauth2TokenStoreServiceImpl redisTokenStoreService = mock(RedisOauth2TokenStoreServiceImpl.class);
        List<Authority> authorities = Arrays.asList(new Authority("TEST"), new Authority("DELETE_TASK"));
        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        Map<String, Object> map = new HashMap<>();
        map.put("fullName", "Test");
        map.put("name", "test@gmail.com");
        map.put("sub", "test@gmail.com");
        map.put("auth", "TEST,DELETE_TASK");
        when(accessToken.getAdditionalInformation()).thenReturn(map);
        when(accessToken.getTokenType()).thenReturn("Bearer");
        when(accessToken.getValue()).thenReturn("Mock JWT");
        when(redisTokenStoreService.getToken(any())).thenReturn(accessToken);

        AccountController accountController = new AccountController(redisTokenStoreService);

        AccountController.UserDto userDto = accountController.index(new AnonymousAuthenticationToken("test", "test", authorities));

        Assertions.assertThat(userDto).isNotNull();
        Assertions.assertThat(userDto.fullName()).isEqualTo("Test");
        Assertions.assertThat(userDto.login()).isEqualTo("test@gmail.com");
        Assertions.assertThat(userDto.email()).isEqualTo("test@gmail.com");
        Assertions.assertThat(userDto.activated()).isTrue();
        Assertions.assertThat(userDto.authorities()).containsExactlyInAnyOrder("TEST", "DELETE_TASK");
    }
}
