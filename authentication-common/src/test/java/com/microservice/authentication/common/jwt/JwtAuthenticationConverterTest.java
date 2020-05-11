package com.microservice.authentication.common.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
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
        Mockito.when(tokenStore.readAuthentication(ArgumentMatchers.anyString())).thenReturn(new OAuth2Authentication(null, authentication));

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
        Mockito.when(tokenStore.readAuthentication(ArgumentMatchers.anyString())).thenReturn(new OAuth2Authentication(null, authentication));

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

        Mockito.verify(tokenStore, Mockito.never()).readAuthentication(ArgumentMatchers.anyString());

        StepVerifier.create(convert)
            .expectNextCount(0)
            .verifyComplete();
    }
}
