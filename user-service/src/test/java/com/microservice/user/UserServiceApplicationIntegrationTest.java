package com.microservice.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.user.dto.UserDto;
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
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.web.reactive.function.BodyInserters.fromValue;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = UserServiceApplication.class,
        properties = {"configuration.swagger=false"})
@AutoConfigureWebTestClient
class UserServiceApplicationIntegrationTest {
    @Autowired
    WebTestClient client;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    DefaultTokenServices defaultTokenServices;

    @Test
    @DisplayName("Test - When Calling GET - /api/users should return list of users and response 200 - OK")
    public void shouldReturnListOfUsersWhenCallApi() {
        String authorizationHeader = authorizationHeader(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        client.get().uri("/api/users")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .exchange()
                .expectStatus().isOk();

        client.get().uri("/api/users?search=fullName:An")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(UserDto.class).hasSize(2);

        client.get().uri("/api/users?search=FULLNAME:Ano")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(UserDto.class).hasSize(1);

        client.get().uri("/api/users?search=fullname:Something else")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

    }

    @Test
    @DisplayName("Test - When Calling POST - /api/users should create a new user and response 201 - Created")
    public void shouldInsertNewUserWhenCallApi() throws Exception {
        String authorizationHeader = authorizationHeader(Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        UserDto userDto = createUserDto();

        client.post().uri("/api/users")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(fromValue(convertToJson(userDto)))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectHeader().value(HttpHeaders.LOCATION, containsString("/api/users/"))
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.createdByUser").isEqualTo("admin@gmail.com")
                .jsonPath("$.activated").isEqualTo(true);
    }

    @Test
    @DisplayName("Test - When Calling PUT - /api/users should update a user and response 200 - OK")
    public void shouldUpdateExistingUserWhenCallApi() throws Exception {
        String authorizationHeader = authorizationHeader(Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")));

        UserDto userDto = createUserDto();
        userDto.setEmail("new@gmail.com");

        String id = client.post().uri("/api/users")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(fromValue(convertToJson(userDto)))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectHeader().value(HttpHeaders.LOCATION, containsString("/api/users/"))
                .expectBody(UserDto.class)
                .returnResult()
                .getResponseBody()
                .getId();

        assertThat(id).isNotEmpty();

        userDto.setFullName("New Name");
        userDto.setPassword(null);
        userDto.setConfirmPassword(null);

        client.put().uri("/api/users/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(fromValue(convertToJson(userDto)))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectHeader().doesNotExist(HttpHeaders.LOCATION)
                .expectBody()
                .jsonPath("$.id").isEqualTo(id)
                .jsonPath("$.fullName").isEqualTo("New Name");
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/users/permissions should return list of permissions and response 200 - OK")
    public void shouldReturnListOfPermissions() {
        String authorizationHeader = authorizationHeader(Arrays.asList(new SimpleGrantedAuthority("SOME_ROLE")));

        client.get().uri("/api/users/permissions")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$..type")
                .value(containsInAnyOrder("Admin Permission", "Person Permissions",
                        "Product Permissions", "Ingredient Permissions",
                        "Category Permissions", "Recipe Permissions",
                        "Task Permissions"));
    }

    private String authorizationHeader(List<SimpleGrantedAuthority> permissions) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken("admin@gmail.com", null, permissions);

        OAuth2Request oAuth2Request = new OAuth2Request(null, authentication.getName(), authentication.getAuthorities(),
                true, Collections.singleton("read"), null, null, null, null);
        OAuth2AccessToken enhance = defaultTokenServices.createAccessToken(new OAuth2Authentication(oAuth2Request, authentication));


        return enhance.getTokenType() + " " + enhance.getValue();
    }

    private String convertToJson(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    private UserDto createUserDto() {
        return UserDto.builder()
                .email("new_user@gmail.com")
                .fullName("Admin")
                .password("Password12345")
                .confirmPassword("Password12345")
                .authorities(Arrays.asList(new UserDto.AuthorityDto("READ")))
                .build();
    }
}