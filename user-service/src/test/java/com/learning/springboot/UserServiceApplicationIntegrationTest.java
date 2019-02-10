package com.learning.springboot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.springboot.config.jwt.TokenProvider;
import com.learning.springboot.dto.UserDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.web.reactive.function.BodyInserters.fromObject;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = UserServiceApplication.class,
        properties = "configuration.swagger=false")
@ActiveProfiles("integration-tests")
@AutoConfigureWebTestClient
class UserServiceApplicationIntegrationTest {
    @Autowired
    WebTestClient client;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    TokenProvider tokenProvider;

    @Test
    @DisplayName("Test - When Cal GET - /api/users should return list of users and response 200 - OK")
    public void shouldReturnListOfUsersWhenCallApi() {
        client.get().uri("/api/users")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader(Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Test - When Cal POST - /api/users should create a new user and response 201 - Created")
    public void shouldInsertNewUserWhenCallApi() throws Exception {
        String authorizationHeader = authorizationHeader(Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        UserDto userDto = createUserDto();

        client.post().uri("/api/users")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(fromObject(convertToJson(userDto)))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectHeader().value(HttpHeaders.LOCATION, containsString("/api/users/"))
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.createdByUser").isEqualTo("master@gmail.com");
    }

    private String authorizationHeader(List<SimpleGrantedAuthority> permissions) {
        return "Bearer " + tokenProvider.createToken(new UsernamePasswordAuthenticationToken("master@gmail.com", null, permissions), "Something", false);
    }

    private String convertToJson(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    private UserDto createUserDto() {
        return UserDto.builder()
                .email("new_user@gmail.com")
                .fullName("Admin")
                .password("12345")
                .confirmPassword("12345")
                .authorities(Arrays.asList(new UserDto.AuthorityDto("READ")))
                .build();
    }
}