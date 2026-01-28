package com.microservice.authentication.controller;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import com.microservice.authentication.autoconfigure.AuthenticationProperties;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import lombok.AllArgsConstructor;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Profile("!prod")
@RestController
@AllArgsConstructor
public class JwksSymmetricController {
	private final AuthenticationProperties authenticationProperties;

	@GetMapping("/.well-known/jwks.json")
	@ResponseBody
	public Map<String, Object> index() {
		OctetSequenceKey key = new OctetSequenceKey.Builder(authenticationProperties.getJwt().getKeyValue().getBytes(StandardCharsets.UTF_8))
				.algorithm(Algorithm.parse("HS256"))
				.keyID("test")
				.build();

//		return new JWKSet(key).toJSONObject();
		return Collections.singletonMap("keys", Collections.singletonList(key.toJSONObject()));
	}
}
