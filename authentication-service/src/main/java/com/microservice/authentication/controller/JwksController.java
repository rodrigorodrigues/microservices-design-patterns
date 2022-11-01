package com.microservice.authentication.controller;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.AllArgsConstructor;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Profile("prod")
@RestController
@AllArgsConstructor
public class JwksController {
	private final KeyPair keyPair;

	@GetMapping("/.well-known/jwks.json")
	@ResponseBody
	public Map<String, Object> index() {
		RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
		RSAKey key = new RSAKey.Builder(publicKey).build();
		return new JWKSet(key).toJSONObject();
	}
}
