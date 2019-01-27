package com.learning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.springboot.AuthenticationServiceApplication;
import com.learning.springboot.dto.LoginDto;
import com.learning.springboot.model.Authority;
import com.learning.springboot.model.User;
import com.learning.springboot.repository.UserRepository;
import com.learning.springboot.util.ReactiveMongoMetadataUtil;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.web.reactive.function.BodyInserters.fromObject;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = AuthenticationServiceApplication.class,
		properties = {"configuration.swagger=false", "debug=true"})
@ActiveProfiles("integration-tests")
@AutoConfigureWebTestClient
@Import(AuthenticationServiceApplicationIntegrationTest.UserMockConfiguration.class)
public class AuthenticationServiceApplicationIntegrationTest {

	@Autowired
	ApplicationContext context;

	@Autowired
	WebTestClient client;

	@Autowired
    ObjectMapper objectMapper;

    @Configuration
    @AllArgsConstructor
    static class UserMockConfiguration {
        private final UserRepository userRepository;

        private final PasswordEncoder passwordEncoder;

        private final ReactiveMongoMetadataUtil reactiveMongoMetadataUtil;

        @PostConstruct
        public void init() {
            userRepository.save(User.builder().email("admin@gmail.com")
                .password(passwordEncoder.encode("password"))
                .authorities(permissions("ROLE_ADMIN"))
                .fullName("Admin dos Santos")
                .build()).subscribe(u -> System.out.println(String.format("Created Admin User: %s", u)));

            userRepository.save(User.builder().email("anonymous@gmail.com")
                .password(passwordEncoder.encode("test"))
                .authorities(permissions("ROLE_READ"))
                .fullName("Anonymous Noname")
                .build()).subscribe(u -> System.out.println(String.format("Created Anonymous User: %s", u)));

            userRepository.save(User.builder().email("master@gmail.com")
                .password(passwordEncoder.encode("password123"))
                .authorities(permissions("ROLE_CREATE", "ROLE_READ", "ROLE_SAVE"))
                .fullName("Master of something")
                .build()).subscribe(u -> System.out.println(String.format("Created Master User: %s", u)));

        }

        private List<Authority> permissions(String ... permissions) {
            return Stream.of(permissions)
                .map(Authority::new)
                .collect(Collectors.toList());
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
    @DisplayName("Test - When Cal POST - /api/authenticate with default system user should return 401 - Unauthorized")
    public void shouldReturnUnauthorizedWhenCallApiWithDefaultSystemUser() {
        client.post().uri("/api/authenticate")
            .body(fromObject(new LoginDto("default@admin.com", "noPassword", false)))
            .exchange()
            .expectStatus().isUnauthorized()
            .expectBody().jsonPath("$.message").value(containsString("User(default@admin.com) not found!"));
    }

}
