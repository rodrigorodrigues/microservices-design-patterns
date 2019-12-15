package com.microservice.authentication.controller;

import com.microservice.authentication.common.model.Authentication;
import com.microservice.authentication.common.model.Authority;
import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountControllerTest {

    @Test
    void testIndex() {
        AuthenticationCommonRepository authenticationCommonRepository = mock(AuthenticationCommonRepository.class);
        List<Authority> authorities = Arrays.asList(new Authority("TEST"), new Authority("DELETE_TASK"));
        when(authenticationCommonRepository.findByEmail(anyString())).thenReturn(Authentication.builder().fullName("Test").email("test@gmail.com").authorities(authorities).build());

        AccountController accountController = new AccountController(authenticationCommonRepository);

        AccountController.UserDto userDto = accountController.index(new AnonymousAuthenticationToken("test", "test", authorities));

        Assertions.assertThat(userDto).isNotNull();
        Assertions.assertThat(userDto.getFullName()).isEqualTo("Test");
        Assertions.assertThat(userDto.getLogin()).isEqualTo("test@gmail.com");
        Assertions.assertThat(userDto.getEmail()).isEqualTo("test@gmail.com");
        Assertions.assertThat(userDto.isActivated()).isTrue();
        Assertions.assertThat(userDto.getAuthorities()).containsExactlyInAnyOrder("TEST", "DELETE_TASK");
    }
}