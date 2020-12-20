package com.microservice.authentication.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwksControllerTest {
    @Test
    void testIndex() {
        RSAPublicKey publicKey = mock(RSAPublicKey.class);
        PrivateKey privateKey = mock(PrivateKey.class);

        when(publicKey.getModulus()).thenReturn(BigInteger.ONE);
        when(publicKey.getPublicExponent()).thenReturn(BigInteger.TEN);

        KeyPair keyPair = new KeyPair(publicKey, privateKey);
        JwksController jwksController = new JwksController(keyPair);

        Map<String, Object> index = jwksController.index();

        assertThat(index).isNotEmpty();
    }
}
