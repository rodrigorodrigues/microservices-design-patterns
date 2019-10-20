package com.microservice.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.authentication.common.model.Authentication;
import com.microservice.authentication.common.model.Authority;
import com.microservice.authentication.dto.JwtTokenDto;
import com.microservice.authentication.repository.AuthenticationRepository;
import com.microservice.web.common.util.constants.DefaultUsers;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.embedded.RedisServer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.web.reactive.function.BodyInserters.fromFormData;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {SharedAuthenticationServiceApplicationIntegrationTest.EmbeddedRedisTestConfiguration.class, AuthenticationServiceApplication.class},
		properties = {"configuration.swagger=false", "debug=debug", "logging.level.com.microservice=debug", "spring.redis.port=6370"})
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

	@Autowired
    ReactiveRedisOperations reactiveRedisOperations;

    @TestConfiguration
    public static class EmbeddedRedisTestConfiguration {

        private final RedisServer redisServer;

        public EmbeddedRedisTestConfiguration(@Value("${spring.redis.port}") final int redisPort) {
            this.redisServer = new RedisServer(redisPort);
        }

        @PostConstruct
        public void startRedis() {
            this.redisServer.start();
        }

        @PreDestroy
        public void stopRedis() {
            this.redisServer.stop();
        }
    }

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
        LinkedMultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", "master@gmail.com");
        formData.add("password", "password123");
        formData.add("rememberMe", "false");
        client.post().uri("/api/authenticate")
            //.body(fromObject(new LoginDto("master@gmail.com", "password123", false)))
            .body(fromFormData(formData))
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectHeader().exists(HttpHeaders.AUTHORIZATION)
            .expectBody().jsonPath("$.id_token").isNotEmpty();
/* // Check later how to test this
        StepVerifier.create(reactiveRedisOperations.keys("*"))
            .expectNextCount(1)
            .verifyComplete();
*/
	}

    @Test
    @DisplayName("Test - When Cal POST - /api/test should be authenticated and response 200 - OK")
    public void shouldUserBeAuthenticatedWhenCallApi() throws IOException {
        LinkedMultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", "master@gmail.com");
        formData.add("password", "password123");
        formData.add("rememberMe", "false");
        JwtTokenDto auth = objectMapper.readValue(client.post().uri("/api/authenticate")
            .body(fromFormData(formData))
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectHeader().exists(HttpHeaders.AUTHORIZATION)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody(), JwtTokenDto.class);

        assertThat(auth).isNotNull();
        assertThat(auth.getIdToken()).isNotEmpty();

        client.get().uri("/api/test")
            .header(HttpHeaders.AUTHORIZATION, auth.getIdToken())
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class).value(containsString("User(master@gmail.com) is authenticated!"));
    }

    @Test
    @DisplayName("Test - When Cal POST - /api/authenticate with default system user should return 401 - Unauthorized")
    public void shouldReturnUnauthorizedWhenCallApiWithDefaultSystemUser() {
        LinkedMultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", DefaultUsers.SYSTEM_DEFAULT.getValue());
        formData.add("password", "noPassword");
        formData.add("rememberMe", "false");
        client.post().uri("/api/authenticate")
            .body(fromFormData(formData))
            .exchange()
            .expectStatus().isUnauthorized()
            .expectBody().jsonPath("$.message").value(containsString("User(default@admin.com) is locked"));
    }

}
