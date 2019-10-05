package com.microservice.user.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.jwt.autoconfigure.JwtCommonAutoConfiguration;
import com.microservice.jwt.common.TokenProvider;
import com.microservice.user.config.SpringSecurityAuditorAware;
import com.microservice.user.config.SpringSecurityConfiguration;
import com.microservice.user.dto.UserDto;
import com.microservice.user.service.UserService;
import com.microservice.web.common.util.HandleResponseError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.web.reactive.function.BodyInserters.fromObject;

@ExtendWith(SpringExtension.class)
@WebFluxTest(properties = {
        "configuration.initialLoad=false",
        "configuration.mongo=false", "debug=true"},
        controllers = UserController.class, excludeAutoConfiguration = MongoReactiveAutoConfiguration.class)
@Import({SpringSecurityConfiguration.class, HandleResponseError.class, ErrorWebFluxAutoConfiguration.class, JwtCommonAutoConfiguration.class})
public class UserControllerTest {

    @Autowired
    WebTestClient client;

    @MockBean
    UserService userService;

    @MockBean
    TokenProvider tokenProvider;

    @MockBean
    SpringSecurityAuditorAware springSecurityAuditorAware;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("Test - When Cal GET - /api/users without valid authorization the response should be 403 - Forbidden")
    @WithMockUser(roles = "INVALID_ROLE")
    public void whenCallFindAllShouldReturnForbiddenWhenDoesNotHavePermission() {
        client.get().uri("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Test - When Cal GET - /api/users without authorization the response should be 401 - Unauthorized")
    public void whenCallFindAllShouldReturnUnauthorizedWhenDoesNotHaveAuthorizationHeader() {
        client.get().uri("/api/users")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Test - When Cal GET - /api/users with valid authorization the response should be a list of Users - 200 - OK")
    @WithMockUser(roles = "ADMIN")
    public void whenCallFindAllShouldReturnListOfUsers() {
        UserDto userDto = new UserDto();
        userDto.setId("100");
        UserDto userDto1 = new UserDto();
        userDto1.setId("200");
        when(userService.findAll()).thenReturn(Flux.fromIterable(Arrays.asList(userDto, userDto1)));

        ParameterizedTypeReference<ServerSentEvent<UserDto>> type = new ParameterizedTypeReference<ServerSentEvent<UserDto>>() {};

        client.get().uri("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM_VALUE)
                .expectBodyList(type)
                .hasSize(2);
    }

    @Test
    @DisplayName("Test - When Cal GET - /api/users/{id} with valid authorization the response should be user - 200 - OK")
    @WithMockUser(roles = "ADMIN")
    public void whenCallFindByIdShouldReturnUser() {
        UserDto userDto = new UserDto();
        userDto.setId("100");
        when(userService.findById(anyString())).thenReturn(Mono.just(userDto));

        client.get().uri("/api/users/{id}", 100)
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody().jsonPath("$.id").value(equalTo("100"));
    }

    @Test
    @DisplayName("Test - When Cal POST - /api/users with valid request should create user and response 201 - Created")
    @WithMockUser(roles = "ADMIN")
    public void whenCallCreateShouldSaveUser() throws Exception {
        UserDto userDto = createUserDto();
        when(userService.save(any(UserDto.class))).thenReturn(Mono.just(userDto));

        client.post().uri("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .contentType(MediaType.APPLICATION_JSON)
                .body(fromObject(convertToJson(userDto)))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody().jsonPath("$.id").value(equalTo(userDto.getId()));
    }

    @Test
    @DisplayName("Test - When Cal PUT - /api/users/{id} with valid authorization the response should be a user - 200 - OK")
    @WithMockUser(roles = "ADMIN")
    public void whenCallUpdateShouldUpdateUser() throws Exception {
        UserDto userDto = createUserDto();
        userDto.setId(UUID.randomUUID().toString());
        userDto.setFullName("New Name");
        when(userService.findById(anyString())).thenReturn(Mono.just(userDto));
        when(userService.save(any(UserDto.class))).thenReturn(Mono.just(userDto));

        client.put().uri("/api/users/{id}", userDto.getId())
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .contentType(MediaType.APPLICATION_JSON)
                .body(fromObject(convertToJson(userDto)))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody().jsonPath("$.id").value(equalTo(userDto.getId()))
                .jsonPath("$.fullName").value(equalTo(userDto.getFullName()));
    }

    @Test
    @DisplayName("Test - When Cal PUT - /api/users/{id} with invalid id the response should be 404 - Not Found")
    @WithMockUser(roles = "ADMIN")
    public void whenCallUpdateShouldResponseNotFound() throws Exception {
        UserDto userDto = createUserDto();
        userDto.setId("999");
        when(userService.findById(anyString())).thenReturn(Mono.empty());

        client.put().uri("/api/users/{id}", userDto.getId())
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .contentType(MediaType.APPLICATION_JSON)
                .body(fromObject(convertToJson(userDto)))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Test - When Cal DELETE - /api/users/{id} with valid authorization the response should be 200 - OK")
    @WithMockUser(roles = "ADMIN")
    public void whenCallDeleteShouldDeleteById() {
        when(userService.deleteById(anyString())).thenReturn(Mono.empty());
        UserDto userDto = new UserDto();
        userDto.setId("12345");

        client.delete().uri("/api/users/{id}", userDto.getId())
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .exchange()
                .expectStatus().is2xxSuccessful();
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