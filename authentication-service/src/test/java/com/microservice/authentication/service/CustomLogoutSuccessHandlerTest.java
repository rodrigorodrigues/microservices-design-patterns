package com.microservice.authentication.service;

import java.io.IOException;

import jakarta.servlet.ServletException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CustomLogoutSuccessHandlerTest {
    @Mock
    RedisTokenStoreServiceImpl redisTokenStoreService;

    @Test
    void testOnLogoutSuccess() throws IOException, ServletException {
        CustomLogoutSuccessHandler handler = new CustomLogoutSuccessHandler(redisTokenStoreService);

        handler.onLogoutSuccess(new MockHttpServletRequest(), new MockHttpServletResponse(), new UsernamePasswordAuthenticationToken("", ""));

        verify(redisTokenStoreService).removeAllTokensByAuthenticationUser(any(Authentication.class));
    }
}
