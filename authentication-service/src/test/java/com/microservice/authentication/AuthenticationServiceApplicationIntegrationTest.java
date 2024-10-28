package com.microservice.authentication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.microservice.authentication.common.model.Authentication;
import com.microservice.authentication.common.model.Authority;
import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import com.microservice.web.common.util.constants.DefaultUsers;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest(classes = AuthenticationServiceApplication.class,
    properties = {"configuration.swagger=false",
        "logging.level.com.microservice=debug",
        "spring.cloud.consul.config.enabled=false",
        "de.flapdoodle.mongodb.embedded.version=5.0.5",
    "logging.level.org.springframework.security=trace"})
@ContextConfiguration(initializers = AuthenticationServiceApplicationIntegrationTest.GenerateKeyPairInitializer.class,
    classes = {AuthenticationServiceApplicationIntegrationTest.UserMockConfiguration.class})
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
public class AuthenticationServiceApplicationIntegrationTest {

    @Autowired
    ApplicationContext context;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    KeyPair keyPair;

    @BeforeEach
    void setup() {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAKey key = new RSAKey.Builder(publicKey).build();

        stubFor(WireMock.get(urlEqualTo("/.well-known/jwks.json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody(new JWKSet(key).toString(false))));
    }

    @TestConfiguration
    @AllArgsConstructor
    static class UserMockConfiguration {
        private final AuthenticationCommonRepository authenticationRepository;

        private final PasswordEncoder passwordEncoder;

        private final MongoTemplate mongoTemplate;

        @PostConstruct
        public void init() {
            mongoTemplate.dropCollection(Authentication.class);

            Authentication authentication = authenticationRepository.save(Authentication.builder().email("master@gmail.com")
                .password(passwordEncoder.encode("password123"))
                .authorities(permissions("ROLE_CREATE", "ROLE_READ", "ROLE_SAVE"))
                .fullName("Master of something")
                .enabled(true)
                .build());
            log.debug(String.format("Created Master Authentication: %s", authentication));
        }

        private List<Authority> permissions(String ... permissions) {
            return Stream.of(permissions)
                .map(Authority::new)
                .collect(Collectors.toList());
        }
    }

    static class GenerateKeyPairInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

        @SneakyThrows
        @Override
        public void initialize(GenericApplicationContext applicationContext) {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair kp = kpg.generateKeyPair();
            RSAPublicKey pub = (RSAPublicKey) kp.getPublic();
            Key pvt = kp.getPrivate();

            Base64.Encoder encoder = Base64.getEncoder();

            Path privateKeyFile = Files.createTempFile("privateKeyFile", ".key");
            Path publicKeyFile = Files.createTempFile("publicKeyFile", ".cert");

            Files.write(privateKeyFile,
                Arrays.asList("-----BEGIN PRIVATE KEY-----", encoder
                    .encodeToString(pvt.getEncoded()), "-----END PRIVATE KEY-----"));
            log.debug("Loaded private key: {}", privateKeyFile);

            Files.write(publicKeyFile,
                Arrays.asList("-----BEGIN PUBLIC KEY-----", encoder
                    .encodeToString(pub.getEncoded()), "-----END PRIVATE KEY-----"));
            log.debug("Loaded public key: {}", publicKeyFile);

            applicationContext.registerBean(KeyPair.class, () -> kp);
            applicationContext.registerBean(ClientRegistrationRepository.class, () -> mock(ClientRegistrationRepository.class));
            applicationContext.registerBean(PublicKey.class, kp::getPublic);
        }
    }

    @Test
    @DisplayName("Test - When Calling POST - /login should be authenticated and response 200 - OK")
    public void shouldUserBeAuthenticatedWhenCallingApi() throws Exception {
        LinkedMultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", "master@gmail.com");
        formData.add("password", "password123");
        mockMvc.perform(post("/login")
            .params(formData)
            .with(csrf()))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Test - When Calling GET - /login should display page and response 200 - OK")
    public void shouldDisplayLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));

    }

    @Test
    @DisplayName("Test - When Calling GET - /api/logout should response 200 - OK")
    public void shouldWorkLogoutUrl() throws Exception {

        mockMvc.perform(get("/api/logout"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/authenticatedUser without jwt should return 401 - Unauthorized")
    public void shouldReturnUnauthorizedWhenCallingApiWithoutAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/authenticatedUser")
            .with(csrf()))
            .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Test - When Calling POST - /api/authenticate with default system user should return 401 - Unauthorized")
    public void shouldReturnUnauthorizedWhenCallingAuthenticateApiWithDefaultSystemUser() throws Exception {
        LinkedMultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", DefaultUsers.SYSTEM_DEFAULT.getValue());
        formData.add("password", "noPassword");

        mockMvc.perform(post("/api/authenticate")
            .params(formData)
            .with(csrf()))
            .andExpect(status().is4xxClientError())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header().doesNotExist(HttpHeaders.AUTHORIZATION))
            .andExpect(jsonPath("$.message", containsString("User is disabled")));
    }

    @Test
    @DisplayName("Test - When Calling GET - / with jwt token should return 200 - Ok")
    public void shouldReturnOkWithToken() throws Exception {
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
            .subject("master@gmail.com")
            .expirationTime(Date.from(ZonedDateTime.now().plusMinutes(1).toInstant()))
            .issueTime(new Date())
            .notBeforeTime(new Date())
            .claim("authorities", Collections.singletonList("ADMIN"))
            .jwtID(UUID.randomUUID().toString())
            .issuer("http://localhost")
            .build();
        JWSSigner signer = new RSASSASigner(keyPair.getPrivate());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("alg", JWSAlgorithm.RS256.getName());
        jsonObject.put("typ", "JWT");
        SignedJWT signedJWT = new SignedJWT(JWSHeader.parse(jsonObject), jwtClaimsSet);
        signedJWT.sign(signer);
        String authorizationHeader = "Bearer " + signedJWT.serialize();

        mockMvc.perform(get("/")
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void shouldWorkLoginWithoutCsrf() throws Exception {
        LinkedMultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", "master@gmail.com");
        formData.add("password", "password123");

        String content = mockMvc.perform(get("/api/csrf"))
                .andExpect(status().is2xxSuccessful())
                .andDo(print())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.headerName", is(notNullValue())))
                .andExpect(jsonPath("$.parameterName", is(notNullValue())))
                .andExpect(jsonPath("$.token", is(notNullValue())))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).isNotEmpty();

        CsrfToken csrfToken = objectMapper.readValue(content, DefaultCsrfToken.class);

        assertThat(csrfToken).isNotNull();

        mockMvc.perform(post("/api/authenticate")
                .params(formData)
                .with(csrf()))
                //.header(csrfToken.getHeaderName(), csrfToken.getToken()))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header().exists(HttpHeaders.AUTHORIZATION))
            .andExpect(jsonPath("$.tokenValue", is(notNullValue())))
            .andExpect(cookie().doesNotExist("SESSIONID"));
    }

    @Test
    @DisplayName("Test - When Calling POST - /api/authenticatedUser should be authenticated and response 200 - OK")
    public void shouldWorkLoginAndLogout() throws Exception {
        LinkedMultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", "master@gmail.com");
        formData.add("password", "password123");

        MvcResult mvcResult = mockMvc.perform(post("/api/authenticate")
                .params(formData)
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header().exists(HttpHeaders.AUTHORIZATION))
            .andExpect(jsonPath("$.tokenValue", is(notNullValue())))
            .andExpect(cookie().doesNotExist("SESSIONID"))
            .andExpect(header().exists("sessionId"))
            .andReturn();

        MockHttpServletResponse response = mvcResult.getResponse();
        String responseBody = response.getContentAsString();

        Map<String, Object> accessToken = objectMapper.readValue(responseBody, Map.class);

        assertThat(accessToken).isNotEmpty();
        assertThat(accessToken.get("tokenValue")).isNotNull();

        String authorization = "Bearer " + accessToken.get("tokenValue").toString();
        mockMvc.perform(get("/api/authenticatedUser")
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .header("sessionId", response.getHeader("sessionId")))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header().exists(HttpHeaders.AUTHORIZATION))
            .andExpect(jsonPath("$.tokenValue", is(notNullValue())));

        formData = new LinkedMultiValueMap<>();
        formData.add("refresh_token", accessToken.get("tokenValue").toString());

        /*mockMvc.perform(post("/api/refreshToken")
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .params(formData))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header().exists(HttpHeaders.AUTHORIZATION))
            .andExpect(jsonPath("$.tokenValue", is(notNullValue())));*/

        mockMvc.perform(get("/api/logout")
                .header(HttpHeaders.AUTHORIZATION, authorization))
            .andExpect(status().isOk())
            .andExpect(cookie().value("SESSIONID", is(nullValue())));

        mockMvc.perform(get("/api/authenticatedUser")
                .with(csrf()))
            .andExpect(status().is4xxClientError());
    }
}
