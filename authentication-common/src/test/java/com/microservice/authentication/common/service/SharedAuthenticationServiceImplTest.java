package com.microservice.authentication.common.service;

import java.util.Optional;

import com.microservice.authentication.common.model.Authentication;
import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class SharedAuthenticationServiceImplTest {

    @Mock
    AuthenticationCommonRepository authenticationCommonRepository;

    SharedAuthenticationServiceImpl sharedAuthenticationService;

    @BeforeEach
    public void setup() {
        sharedAuthenticationService = new SharedAuthenticationServiceImpl(authenticationCommonRepository);
        Mockito.when(authenticationCommonRepository.findByEmail(anyString())).thenReturn(Optional.of(new Authentication()));
    }

    @Test
    void testLoadUserByUsername() {
        UserDetails userDetails = sharedAuthenticationService.loadUserByUsername(anyString());

        assertThat(userDetails).isNotNull();
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {
        Mockito.when(authenticationCommonRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        Assertions.assertThrows(UsernameNotFoundException.class, () -> sharedAuthenticationService.loadUserByUsername("test"), "Authentication(test) not found!");
    }

}
