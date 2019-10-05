package com.microservice.authentication.jwt.common;

import com.microservice.jwt.common.JwtAuthenticationConverter;
import com.microservice.jwt.common.TokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationConverterTest {

    @Mock
    TokenProvider tokenProvider;

    JwtAuthenticationConverter jwtAuthenticationConverter;

    @BeforeEach
    public void setup() {
        jwtAuthenticationConverter = new JwtAuthenticationConverter(tokenProvider);
    }

    @Test
    void shouldValidateByAuthorizationHeaderAndReturnValidAuthentication() {
        when(tokenProvider.validateToken(anyString())).thenReturn(true);
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(null, null);
        when(tokenProvider.getAuthentication(anyString())).thenReturn(authenticationToken);

        MockServerHttpRequest.BaseBuilder<?> baseBuilder = MockServerHttpRequest.get("/anything")
            .header(HttpHeaders.AUTHORIZATION, "Bearer JWT");

        Mono<Authentication> convert = jwtAuthenticationConverter.convert(MockServerWebExchange.from(baseBuilder));

        StepVerifier.create(convert)
            .expectNext(authenticationToken)
            .verifyComplete();
    }

    @Test
    void shouldValidateByRequestParameterAndReturnValidAuthentication() {
        when(tokenProvider.validateToken(anyString())).thenReturn(true);
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(null, null);
        when(tokenProvider.getAuthentication(anyString())).thenReturn(authenticationToken);

        MockServerHttpRequest.BaseBuilder<?> baseBuilder = MockServerHttpRequest.get("/anything")
            .queryParam(HttpHeaders.AUTHORIZATION, "Bearer JWT");

        Mono<Authentication> convert = jwtAuthenticationConverter.convert(MockServerWebExchange.from(baseBuilder));

        StepVerifier.create(convert)
            .expectNext(authenticationToken)
            .verifyComplete();
    }

    @Test
    void shouldReturnEmptyAuthenticationWhenTokenIsInvalid() {
        when(tokenProvider.validateToken(anyString())).thenReturn(false);

        MockServerHttpRequest.BaseBuilder<?> baseBuilder = MockServerHttpRequest.get("/anything")
            .queryParam(HttpHeaders.AUTHORIZATION, "Bearer Invalid JWT");

        Mono<Authentication> convert = jwtAuthenticationConverter.convert(MockServerWebExchange.from(baseBuilder));

        verify(tokenProvider, never()).getAuthentication(anyString());

        StepVerifier.create(convert)
            .expectNextCount(0)
            .verifyComplete();
    }

    @Test
    void shouldReturnEmptyAuthenticationWhenTokenDoesNotStartWithBearer() {
        MockServerHttpRequest.BaseBuilder<?> baseBuilder = MockServerHttpRequest.get("/anything")
            .queryParam(HttpHeaders.AUTHORIZATION, "base64 JWT");

        Mono<Authentication> convert = jwtAuthenticationConverter.convert(MockServerWebExchange.from(baseBuilder));

        verify(tokenProvider, never()).getAuthentication(anyString());

        StepVerifier.create(convert)
            .expectNextCount(0)
            .verifyComplete();
    }
}
