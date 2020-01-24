package com.microservice.authentication.jwt.common;

import com.microservice.jwt.common.JwtAuthenticationConverter;
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
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenStore;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationConverterTest {

    @Mock
    TokenStore tokenStore;

    JwtAuthenticationConverter jwtAuthenticationConverter;

    @BeforeEach
    public void setup() {
        jwtAuthenticationConverter = new JwtAuthenticationConverter(tokenStore);
    }

    @Test
    void shouldValidateByAuthorizationHeaderAndReturnValidAuthentication() {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(null, null);
        when(tokenStore.readAuthentication(anyString())).thenReturn(new OAuth2Authentication(null, authentication));

        MockServerHttpRequest.BaseBuilder<?> baseBuilder = MockServerHttpRequest.get("/anything")
            .header(HttpHeaders.AUTHORIZATION, "Bearer JWT");

        Mono<Authentication> convert = jwtAuthenticationConverter.convert(MockServerWebExchange.from(baseBuilder));

        StepVerifier.create(convert)
            .expectNext(authentication)
            .verifyComplete();
    }

    @Test
    void shouldValidateByRequestParameterAndReturnValidAuthentication() {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(null, null);
        when(tokenStore.readAuthentication(anyString())).thenReturn(new OAuth2Authentication(null, authentication));

        MockServerHttpRequest.BaseBuilder<?> baseBuilder = MockServerHttpRequest.get("/anything")
            .queryParam(HttpHeaders.AUTHORIZATION, "Bearer JWT");

        Mono<Authentication> convert = jwtAuthenticationConverter.convert(MockServerWebExchange.from(baseBuilder));

        StepVerifier.create(convert)
            .expectNext(authentication)
            .verifyComplete();
    }

    @Test
    void shouldReturnEmptyAuthenticationWhenTokenIsInvalid() {
        MockServerHttpRequest.BaseBuilder<?> baseBuilder = MockServerHttpRequest.get("/anything")
            .queryParam(HttpHeaders.AUTHORIZATION, "Bearer Invalid JWT");

        Mono<Authentication> convert = jwtAuthenticationConverter.convert(MockServerWebExchange.from(baseBuilder));

        StepVerifier.create(convert)
            .expectNextCount(0)
            .expectError()
            .verify();
    }

    @Test
    void shouldReturnEmptyAuthenticationWhenTokenDoesNotStartWithBearer() {
        MockServerHttpRequest.BaseBuilder<?> baseBuilder = MockServerHttpRequest.get("/anything")
            .queryParam(HttpHeaders.AUTHORIZATION, "base64 JWT");

        Mono<Authentication> convert = jwtAuthenticationConverter.convert(MockServerWebExchange.from(baseBuilder));

        verify(tokenStore, never()).readAuthentication(anyString());

        StepVerifier.create(convert)
            .expectNextCount(0)
            .verifyComplete();
    }
}
