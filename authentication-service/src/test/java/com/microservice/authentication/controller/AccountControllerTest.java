package com.microservice.authentication.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.microservice.authentication.common.model.Authentication;
import com.microservice.authentication.common.model.Authority;
import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import com.microservice.authentication.service.GenerateToken;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountControllerTest {

    @Test
    void testIndex() {
        SessionRepository sessionRepository = mock(SessionRepository.class);
        GenerateToken generateToken = mock(GenerateToken.class);
        List<GrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority("TEST"), new SimpleGrantedAuthority("DELETE_TASK"));
        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        AuthenticationCommonRepository authenticationCommonRepository = mock(AuthenticationCommonRepository.class);
        Map<String, Object> map = new HashMap<>();
        map.put("fullName", "Test");
        map.put("name", "test@gmail.com");
        map.put("sub", "test@gmail.com");
        map.put("auth", "TEST,DELETE_TASK");
        when(accessToken.getTokenType()).thenReturn(OAuth2AccessToken.TokenType.BEARER);
        when(accessToken.getTokenValue()).thenReturn("Mock JWT");
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("1");
        when(sessionRepository.findById(anyString())).thenReturn(session);
        when(session.getAttribute(anyString())).thenReturn(accessToken);

        AccountController accountController = new AccountController(sessionRepository, generateToken, authenticationCommonRepository);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession mockHttpSession = new MockHttpSession();
        request.setSession(mockHttpSession);
        Authentication principal = new Authentication();
        principal.setFullName("Test");
        principal.setEmail("test@gmail.com");
        principal.setAuthorities(authorities.stream().map(a -> new Authority(a.getAuthority())).collect(Collectors.toList()));
        when(authenticationCommonRepository.findByEmail(anyString())).thenReturn(Optional.of(principal));

        AccountController.UserDto userDto = accountController.index(new AnonymousAuthenticationToken("Test", principal, authorities), request);

        Assertions.assertThat(userDto).isNotNull();
        Assertions.assertThat(userDto.fullName()).isEqualTo("Test");
        Assertions.assertThat(userDto.login()).isEqualTo("test@gmail.com");
        Assertions.assertThat(userDto.email()).isEqualTo("test@gmail.com");
        Assertions.assertThat(userDto.activated()).isTrue();
        Assertions.assertThat(userDto.authorities()).containsExactlyInAnyOrder("TEST", "DELETE_TASK");
    }
}
