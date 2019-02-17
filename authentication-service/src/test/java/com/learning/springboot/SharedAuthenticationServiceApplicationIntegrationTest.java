package com.learning.springboot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.springboot.constants.DefaultUsers;
import com.learning.springboot.dto.LoginDto;
import com.learning.springboot.model.Authentication;
import com.learning.springboot.model.Authority;
import com.learning.springboot.repository.AuthenticationRepository;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.web.reactive.function.BodyInserters.fromObject;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = AuthenticationServiceApplication.class,
		properties = "configuration.swagger=false")
@ActiveProfiles("integration-tests")
@AutoConfigureWebTestClient
@Import(SharedAuthenticationServiceApplicationIntegrationTest.UserMockConfiguration.class)
public class SharedAuthenticationServiceApplicationIntegrationTest {

	@Autowired
	ApplicationContext context;

	@Autowired
	WebTestClient client;

	@Autowired
    ObjectMapper objectMapper;

    @Configuration
    @AllArgsConstructor
    static class UserMockConfiguration {
        private final AuthenticationRepository authenticationRepository;

        private final PasswordEncoder passwordEncoder;

        @PostConstruct
        public void init() {
            authenticationRepository.save(Authentication.builder().email("master@gmail.com")
                .password(passwordEncoder.encode("password123"))
                .authorities(permissions("ROLE_CREATE", "ROLE_READ", "ROLE_SAVE"))
                .fullName("Master of something")
                .enabled(true)
                .build()).subscribe(u -> System.out.println(String.format("Created Master Authentication: %s", u)));

        }

        private List<Authority> permissions(String ... permissions) {
            return Stream.of(permissions)
                .map(Authority::new)
                .collect(Collectors.toList());
        }

        @RestController
        @RequestMapping("/api/test")
        class TestController {
            @GetMapping
            public ResponseEntity<String> get(@AuthenticationPrincipal org.springframework.security.core.Authentication authentication) {
                return ResponseEntity.ok(String.format("User(%s) is authenticated!", authentication.getName()));
            }
        }
    }

    @Test
	@DisplayName("Test - When Cal POST - /api/authenticate should return token and response 200 - OK")
	public void shouldReturnTokenWhenCallApi() {
        client.post().uri("/api/authenticate")
            .body(fromObject(new LoginDto("master@gmail.com", "password123", false)))
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectHeader().exists(HttpHeaders.AUTHORIZATION)
            .expectBody().jsonPath("$.id_token").isNotEmpty();
	}

    @Test
    @DisplayName("Test - When Cal POST - /api/test should be authenticated and response 200 - OK")
    public void shouldUserBeAuthenticatedWhenCallApi() throws IOException {
        Map<String, String> auth = objectMapper.readValue(client.post().uri("/api/authenticate")
            .body(fromObject(new LoginDto("master@gmail.com", "password123", false)))
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectHeader().exists(HttpHeaders.AUTHORIZATION)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody(), Map.class);

        assertThat(auth.isEmpty()).isFalse();
        assertThat(auth.get("id_token")).isNotEmpty();

        client.get().uri("/api/test")
            .header(HttpHeaders.AUTHORIZATION, auth.get("id_token"))
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class).value(containsString("User(master@gmail.com) is authenticated!"));
    }

    @Test
    @DisplayName("Test - When Cal POST - /api/authenticate with default system user should return 401 - Unauthorized")
    public void shouldReturnUnauthorizedWhenCallApiWithDefaultSystemUser() {
        client.post().uri("/api/authenticate")
            .body(fromObject(new LoginDto(DefaultUsers.SYSTEM_DEFAULT.getValue(), "noPassword", false)))
            .exchange()
            .expectStatus().isUnauthorized()
            .expectBody().jsonPath("$.message").value(containsString("User(default@admin.com) is locked"));
    }

}
