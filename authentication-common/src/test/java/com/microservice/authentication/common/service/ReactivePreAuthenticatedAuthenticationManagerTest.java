package com.microservice.authentication.common.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class ReactivePreAuthenticatedAuthenticationManagerTest {

    @Mock
    ReactiveUserDetailsService userDetailsService;

    @Mock
    UserDetailsChecker userDetailsChecker;

    ReactivePreAuthenticatedAuthenticationManager reactivePreAuthenticatedAuthenticationManager;

    @BeforeEach
    public void setup() {
        reactivePreAuthenticatedAuthenticationManager = new ReactivePreAuthenticatedAuthenticationManager(userDetailsService, userDetailsChecker);
    }

    @Test
    void testAuthenticate() {
        Mockito.when(userDetailsService.findByUsername(anyString())).thenReturn(Mono.just(new com.microservice.authentication.common.model.Authentication()));

        Mono<Authentication> authenticate = reactivePreAuthenticatedAuthenticationManager.authenticate(new UsernamePasswordAuthenticationToken("test", "test"));

        StepVerifier.create(authenticate)
            .expectNextCount(1L)
            .verifyComplete();
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {
        Mockito.when(userDetailsService.findByUsername(anyString())).thenReturn(Mono.empty());

        Mono<Authentication> authenticate = reactivePreAuthenticatedAuthenticationManager.authenticate(new UsernamePasswordAuthenticationToken("test", "test"));

        StepVerifier.create(authenticate)
            .expectErrorMessage("User(test) not found")
            .verify();
    }
}
