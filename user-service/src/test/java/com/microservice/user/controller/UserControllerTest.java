package com.microservice.user.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.microservice.authentication.autoconfigure.AuthenticationCommonConfiguration;
import com.microservice.authentication.resourceserver.config.ActuatorResourceServerConfiguration;
import com.microservice.user.UserServiceApplicationIntegrationTest;
import com.microservice.user.config.SpringSecurityAuditorAware;
import com.microservice.user.dto.UserDto;
import com.microservice.user.repository.UserRepository;
import com.microservice.user.service.UserService;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.querydsl.core.types.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(properties = {
        "configuration.initialLoad=false",
        "configuration.mongo=false"},
        controllers = UserController.class, excludeAutoConfiguration = MongoAutoConfiguration.class)
@ContextConfiguration(initializers = {UserServiceApplicationIntegrationTest.GenerateKeyPairInitializer.class, UserControllerTest.JwtDecoderInitializer.class})
@AutoConfigureWireMock(port = 0)
@Import({AuthenticationCommonConfiguration.class, ActuatorResourceServerConfiguration.class})
@EnableConfigurationProperties(ResourceServerProperties.class)
public class UserControllerTest {

    @Autowired
    MockMvc client;

    @MockBean
    UserService userService;

    @MockBean
    TokenStore tokenStore;

    @MockBean
    SpringSecurityAuditorAware springSecurityAuditorAware;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    KeyPair keyPair;

    AtomicBoolean runAtOnce = new AtomicBoolean(true);

    static class JwtDecoderInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

        @Override
        public void initialize(GenericApplicationContext applicationContext) {
            applicationContext.registerBean(UserRepository.class, () -> mock(UserRepository.class));
        }
    }

    @BeforeEach
    public void setup() {
        if (runAtOnce.getAndSet(false)) {
            RSAKey.Builder builder = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .keyID("test");
            JWKSet jwkSet = new JWKSet(builder.build());

            String jsonPublicKey = jwkSet.toJSONObject().toJSONString();
            stubFor(WireMock.get(urlPathEqualTo("/.well-known/jwks.json"))
                    .willReturn(aResponse().withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).withBody(jsonPublicKey)));
        }
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/users without valid authorization the response should be 403 - Forbidden")
    @WithMockUser(roles = "INVALID_ROLE")
    public void whenCallFindAllShouldReturnForbiddenWhenDoesNotHavePermission() throws Exception {
        client.perform(get("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/users without authorization the response should be 401 - Unauthorized")
    public void whenCallFindAllShouldReturnUnauthorizedWhenDoesNotHaveAuthorizationHeader() throws Exception {
        client.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/users with valid authorization the response should be a list of Users - 200 - OK")
    @WithMockUser(roles = "ADMIN")
    public void whenCallFindAllShouldReturnListOfUsers() throws Exception {
        UserDto userDto = new UserDto();
        userDto.setId("100");
        UserDto userDto1 = new UserDto();
        userDto1.setId("200");
        when(userService.findAll(any(Pageable.class), any(Predicate.class))).thenReturn(new PageImpl<>(Arrays.asList(userDto, userDto1)));

        ParameterizedTypeReference<ServerSentEvent<UserDto>> type = new ParameterizedTypeReference<ServerSentEvent<UserDto>>() {};

        client.perform(get("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$..id", hasSize(2)));
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/users/{id} with valid authorization the response should be user - 200 - OK")
    @WithMockUser(roles = "ADMIN")
    public void whenCallFindByIdShouldReturnUser() throws Exception {
        UserDto userDto = new UserDto();
        userDto.setId("100");
        when(userService.findById(anyString())).thenReturn(userDto);

        client.perform(get("/api/users/{id}", 100)
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is("100")));
    }

    @Test
    @DisplayName("Test - When Calling POST - /api/users with valid request should create user and response 201 - Created")
    @WithMockUser(roles = "ADMIN")
    public void whenCallCreateShouldSaveUser() throws Exception {
        UserDto userDto = createUserDto();
        when(userService.save(any(UserDto.class))).thenReturn(userDto);

        client.perform(MockMvcRequestBuilders.post("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertToJson(userDto)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", equalTo(userDto.getId())));
    }

    @Test
    @DisplayName("Test - When Calling PUT - /api/users/{id} with valid authorization the response should be a user - 200 - OK")
    @WithMockUser(roles = "ADMIN")
    public void whenCallUpdateShouldUpdateUser() throws Exception {
        UserDto userDto = createUserDto();
        userDto.setId(UUID.randomUUID().toString());
        userDto.setFullName("New Name");
        when(userService.findById(anyString())).thenReturn(userDto);
        when(userService.save(any(UserDto.class))).thenReturn(userDto);

        client.perform(put("/api/users/{id}", userDto.getId())
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertToJson(userDto)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", equalTo(userDto.getId())))
                .andExpect(jsonPath("$.fullName", equalTo(userDto.getFullName())));
    }

    @Test
    @DisplayName("Test - When Calling PUT - /api/users/{id} with invalid id the response should be 404 - Not Found")
    @WithMockUser(roles = "ADMIN")
    public void whenCallUpdateShouldResponseNotFound() throws Exception {
        UserDto userDto = createUserDto();
        userDto.setId("999");

        client.perform(put("/api/users/{id}", userDto.getId())
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertToJson(userDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Test - When Calling DELETE - /api/users/{id} with valid authorization the response should be 200 - OK")
    @WithMockUser(roles = "ADMIN")
    public void whenCallDeleteShouldDeleteById() throws Exception {
        UserDto userDto = new UserDto();
        userDto.setId("12345");
        when(userService.findById(anyString())).thenReturn(userDto);

        client.perform(delete("/api/users/{id}", userDto.getId())
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT"))
                .andExpect(status().is2xxSuccessful());

        verify(userService).deleteById(anyString());
    }

    @Test
    @DisplayName("Test - When Calling DELETE - /api/users/{id} with id that does not exist should response 404 - Not Found")
    @WithMockUser(roles = "ADMIN")
    public void whenCallDeleteShouldResponseNotFound() throws Exception {
        client.perform(delete("/api/users/{id}", "12345")
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT"))
                .andExpect(status().isNotFound());
    }

    private String convertToJson(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    private UserDto createUserDto() {
        return UserDto.builder()
                .id(UUID.randomUUID().toString())
                .email("admin@gmail.com")
                .fullName("Admin")
                .build();
    }
}