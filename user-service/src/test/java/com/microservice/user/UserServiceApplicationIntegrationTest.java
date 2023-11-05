package com.microservice.user;

import java.text.ParseException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.authentication.autoconfigure.AuthenticationProperties;
import com.microservice.authentication.common.model.Authority;
import com.microservice.user.dto.UserDto;
import com.microservice.user.model.User;
import com.microservice.user.repository.UserRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = UserServiceApplication.class,
        properties = {"configuration.swagger=false", "de.flapdoodle.mongodb.embedded.version=5.0.5"})
@ContextConfiguration(classes = UserServiceApplicationIntegrationTest.PopulateDbConfiguration.class)
@AutoConfigureMockMvc
public class UserServiceApplicationIntegrationTest {
    @Autowired
    MockMvc client;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    AuthenticationProperties authenticationProperties;

    @TestConfiguration
    static class PopulateDbConfiguration {
        @Bean
        CommandLineRunner runner(PasswordEncoder passwordEncoder, UserRepository userRepository) {
            return args -> {
                userRepository.saveAll(Arrays.asList(User.builder().email("admin@gmail.com")
                        .password(passwordEncoder.encode("password"))
                        .authorities(permissions("ROLE_ADMIN"))
                        .fullName("Admin dos Santos")
                        .build(), User.builder().email("anonymous@gmail.com")
                        .password(passwordEncoder.encode("test"))
                        .authorities(permissions("ROLE_PERSON_READ"))
                        .fullName("Anonymous Noname")
                        .build(), User.builder().email("master@gmail.com")
                        .password(passwordEncoder.encode("password123"))
                        .authorities(permissions("ROLE_PERSON_CREATE", "ROLE_PERSON_READ", "ROLE_PERSON_SAVE"))
                        .fullName("Master of something")
                        .build()));
            };
        }

        private List<Authority> permissions(String ... permissions) {
            return Stream.of(permissions)
                    .map(Authority::new)
                    .collect(Collectors.toList());
        }
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/users should return list of users and response 200 - OK")
    public void shouldReturnListOfUsersWhenCallApi() throws Exception {
        String authorizationHeader = authorizationHeader(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        client.perform(get("/api/users")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
                .andExpect(status().isOk());

        client.perform(get("/api/users?fullName=An")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(2)))
                .andExpect(jsonPath("$.content[*].id", hasSize(2)));

        client.perform(get("/api/users?fullName=Ano")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(1)))
                .andExpect(jsonPath("$.content[*].id", hasSize(1)));

        client.perform(get("/api/users?fullName=Something else")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(0)))
                .andExpect(jsonPath("$.content", empty()));
    }

    @Test
    @DisplayName("Test - When Calling POST - /api/users should create a new user and response 201 - Created")
    public void shouldInsertNewUserWhenCallApi() throws Exception {
        String authorizationHeader = authorizationHeader(Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        UserDto userDto = createUserDto();

        client.perform(MockMvcRequestBuilders.post("/api/users")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertToJson(userDto)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/users/")))
                .andExpect(jsonPath("$.[*].id", notNullValue()))
                .andExpect(jsonPath("$.createdByUser", equalTo("admin@gmail.com")))
                .andExpect(jsonPath("$.activated", equalTo(true)));
    }

    @Test
    @DisplayName("Test - When Calling PUT - /api/users should update a user and response 200 - OK")
    public void shouldUpdateExistingUserWhenCallApi() throws Exception {
        String authorizationHeader = authorizationHeader(Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")));

        UserDto userDto = createUserDto();
        userDto.setEmail("new@gmail.com");

        UserDto userDtoResponse = objectMapper.readValue(client.perform(MockMvcRequestBuilders.post("/api/users")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertToJson(userDto)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/users/")))
                .andReturn()
                .getResponse()
                .getContentAsString(), UserDto.class);

        assertThat(userDtoResponse).isNotNull();
        String id = userDtoResponse.getId();
        assertThat(id).isNotEmpty();

        userDto.setFullName("New Name");
        userDto.setPassword(null);
        userDto.setConfirmPassword(null);

        client.perform(MockMvcRequestBuilders.put("/api/users/{id}", id)
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
            .contentType(MediaType.APPLICATION_JSON)
            .content(convertToJson(userDto)))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(header().doesNotExist(HttpHeaders.LOCATION))
            .andExpect(jsonPath("$.id", equalTo(id)))
            .andExpect(jsonPath("$.fullName", equalTo("New Name")));
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/users/permissions should return list of permissions and response 200 - OK")
    public void shouldReturnListOfPermissions() throws Exception {
        String authorizationHeader = authorizationHeader(Arrays.asList(new SimpleGrantedAuthority("SOME_ROLE")));

        client.perform(get("/api/users/permissions")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$..type", containsInAnyOrder("Admin Permission", "Person Permissions",
                        "Product Permissions", "Ingredient Permissions",
                        "Category Permissions", "Recipe Permissions",
                        "Task Permissions", "Post Permissions")));
    }

    private String authorizationHeader(List<SimpleGrantedAuthority> permissions) throws ParseException, JOSEException {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken("admin@gmail.com", null, permissions);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(authentication.getName())
                .expirationTime(Date.from(ZonedDateTime.now().plusMinutes(1).toInstant()))
                .issueTime(new Date())
                .notBeforeTime(new Date())
                .claim("authorities", authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()))
                .jwtID(UUID.randomUUID().toString())
                .issuer("jwt")
                .build();
        JWSSigner signer = new MACSigner(authenticationProperties.getJwt().getKeyValue());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("kid", "test");
        jsonObject.put("alg", JWSAlgorithm.HS256.getName());
        jsonObject.put("typ", "JWT");
        SignedJWT signedJWT = new SignedJWT(JWSHeader.parse(jsonObject), jwtClaimsSet);
        signedJWT.sign(signer);
        return "Bearer " + signedJWT.serialize();
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