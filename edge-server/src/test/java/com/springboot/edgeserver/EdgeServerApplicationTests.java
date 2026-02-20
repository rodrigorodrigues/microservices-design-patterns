package com.springboot.edgeserver;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import com.microservice.authentication.autoconfigure.AuthenticationProperties;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.TestcontainersConfiguration;
import org.wiremock.spring.EnableWireMock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.session.ReactiveSessionRepository;
import org.springframework.session.Session;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "grafanaUrl=http://localhost:${wiremock.server.port}/admin/grafana"
        })
@EnableWireMock
@ContextConfiguration(classes = EdgeServerApplicationTests.MockConfiguration.class)
class EdgeServerApplicationTests {

    @Autowired
    ApplicationContext context;
    @Autowired
    JwtEncoder jwtEncoder;
    @Autowired
    ReactiveSessionRepository<?> sessionRepository;
    private WebTestClient webClient;

    static {
        GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:5.0.3-alpine")).withExposedPorts(6379);
        redis.start();
        System.setProperty("spring.data.redis.host", redis.getHost());
        System.setProperty("spring.data.redis.port", redis.getMappedPort(6379).toString());
    }

    @TestConfiguration
    static class MockConfiguration {
        @Bean
        JwtEncoder jwtEncoder(AuthenticationProperties properties) {
            AuthenticationProperties.Jwt jwt = properties.getJwt();
            String keyValue = jwt.getKeyValue();
            return parameters -> {
                byte[] secret = keyValue.getBytes(StandardCharsets.UTF_8);
                SecretKeySpec secretKeySpec = new SecretKeySpec(secret, "HMACSHA256");

                try {
                    MACSigner signer = new MACSigner(secretKeySpec);

                    JWTClaimsSet.Builder claimsSetBuilder = new JWTClaimsSet.Builder();
                    parameters.getClaims().getClaims().forEach((key, value) ->
                            claimsSetBuilder.claim(key, value instanceof Instant ? Date.from((Instant) value) : value)
                    );
                    JWTClaimsSet claimsSet = claimsSetBuilder.build();

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("alg", JWSAlgorithm.HS256.getName());
                    jsonObject.put("typ", "JWT");
                    jsonObject.put("kid", "test");

                    JWSHeader header = JWSHeader.parse(jsonObject);
                    SignedJWT signedJWT = new SignedJWT(header, claimsSet);
                    signedJWT.sign(signer);

                    return Jwt.withTokenValue(signedJWT.serialize())
                            .header("alg", header.getAlgorithm().getName())
                            .header("typ", "JWT")
                            .header("kid", header.getKeyID())
                            .subject(claimsSet.getSubject())
                            .issuer(claimsSet.getIssuer())
                            .claims(claims -> claims.putAll(claimsSet.getClaims()))
                            .issuedAt(claimsSet.getIssueTime().toInstant())
                            .expiresAt(claimsSet.getExpirationTime().toInstant())
                            .build();
                }
                catch (Exception e) {
                    throw new IllegalStateException("Error while signing the JWT", e);
                }
            };
        }
    }

    @BeforeEach
    public void setup() {
        this.webClient = WebTestClient
                .bindToApplicationContext(this.context)
                // add Spring Security test Support
                .apply(springSecurity())
                .configureClient()
                .build();
    }

    @Test
    public void contextLoads() {
        // Manually create a session and save it - cast to raw type to avoid generic issues
        ReactiveSessionRepository rawRepo = sessionRepository;
        Object sessionObj = rawRepo.createSession().block();

        // The session is actually a RedisSession, but we can use it as Session interface
        Session session = (Session) sessionObj;
        JwtClaimsSet jwtClaimsSet = JwtClaimsSet.builder()
                .subject("dummy")
                .expiresAt(Date.from(ZonedDateTime.now().plusMinutes(1).toInstant()).toInstant())
                .issuedAt(Instant.now())
                .notBefore(Instant.now())
                .claim("authorities", List.of("ROLE_ADMIN"))
                .id(UUID.randomUUID().toString())
                .issuer("jwt")
                .build();
        Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(jwtClaimsSet));
        OAuth2AccessToken token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                jwt.getTokenValue(),
                jwt.getIssuedAt(),
                jwt.getExpiresAt(),
                new HashSet<>(jwt.getClaimAsStringList("authorities")));
        session.setAttribute("token", token);
        rawRepo.save(session).block();

        String sessionId = session.getId();

        stubFor(get(urlPathMatching("/admin/grafana/.*"))
                .willReturn(aResponse()
                        .withBody("{\"manual\":\"session\"}")
                        .withHeader("Content-Type", "application/json")));

        // Use the session ID in a request
        webClient
                .mutateWith(mockUser().roles("ADMIN"))
                .get().uri("/admin/grafana/test")
                .cookie("SESSIONID", sessionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.manual").isEqualTo("session");

        verify(getRequestedFor(urlPathMatching("/admin/grafana/.*")).withHeader("Authorization", containing("Bearer "))
                .withCookie("SESSIONID", containing(sessionId)));
    }

}