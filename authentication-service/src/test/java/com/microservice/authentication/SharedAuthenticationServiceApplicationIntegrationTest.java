package com.microservice.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.authentication.common.model.Authentication;
import com.microservice.authentication.common.model.Authority;
import com.microservice.authentication.dto.JwtTokenDto;
import com.microservice.web.common.util.constants.DefaultUsers;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.repository.Repository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import redis.embedded.RedisServer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {SharedAuthenticationServiceApplicationIntegrationTest.EmbeddedRedisTestConfiguration.class, AuthenticationServiceApplication.class},
		properties = {"configuration.swagger=false", "debug=debug", "logging.level.com.microservice=debug", "spring.redis.port=6370"})
@ActiveProfiles("integration-tests")
@AutoConfigureWebTestClient
@Import({SharedAuthenticationServiceApplicationIntegrationTest.MockAuthenticationMongoConfiguration.class, SharedAuthenticationServiceApplicationIntegrationTest.UserMockConfiguration.class})
@AutoConfigureMockMvc
public class SharedAuthenticationServiceApplicationIntegrationTest {

	@Autowired
	ApplicationContext context;

	@Autowired
    MockMvc mockMvc;

	@Autowired
    ObjectMapper objectMapper;

	@Autowired @Qualifier("stringRedisTemplate")
    RedisOperations redisOperations;

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

    @TestConfiguration
    @EnableMongoAuditing
    @EnableMongoRepositories(basePackageClasses = AuthenticationRepository.class, considerNestedRepositories = true)
    static class MockAuthenticationMongoConfiguration {
    }

    interface AuthenticationRepository extends Repository<Authentication, String> {
        Authentication save(Authentication authentication);
    }

    @TestConfiguration
    @AllArgsConstructor
    static class UserMockConfiguration {
        private final AuthenticationRepository authenticationRepository;

        private final PasswordEncoder passwordEncoder;

        @PostConstruct
        public void init() {
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

    @Test
	@DisplayName("Test - When Cal POST - /api/authenticate should return token and response 200 - OK")
	public void shouldReturnTokenWhenCallApi() throws Exception {
        LinkedMultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", "master@gmail.com");
        formData.add("password", "password123");
        formData.add("rememberMe", "false");
        String sessionId = mockMvc.perform(post("/api/authenticate")
            .params(formData))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header().exists(HttpHeaders.AUTHORIZATION))
            .andExpect(jsonPath("$.id_token", is(notNullValue())))
            .andReturn()
            .getResponse()
            .getHeader("sessionId");

        assertThat(sessionId).isNotEmpty();
        Set keys = redisOperations.keys(String.format("spring:session:sessions:%s*", sessionId));
        assertThat(keys).isNotNull();
        assertThat(keys.size()).isEqualTo(1);
	}

    @Test
    @DisplayName("Test - When Cal POST - /api/authenticatedUser should be authenticated and response 200 - OK")
    public void shouldUserBeAuthenticatedWhenCallApi() throws Exception {
        LinkedMultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", "master@gmail.com");
        formData.add("password", "password123");
        formData.add("rememberMe", "false");

        MvcResult mvcResult = mockMvc.perform(post("/api/authenticate")
            .params(formData))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header().exists(HttpHeaders.AUTHORIZATION))
            .andExpect(jsonPath("$.id_token", is(notNullValue())))
            .andExpect(cookie().value("SESSIONID", is(notNullValue())))
            .andReturn();

        MockHttpServletResponse response = mvcResult.getResponse();
        String responseBody = response
            .getContentAsString();

        JwtTokenDto auth = objectMapper.readValue(responseBody, JwtTokenDto.class);

        assertThat(auth).isNotNull();
        assertThat(auth.getIdToken()).isNotEmpty();

        mockMvc.perform(get("/api/authenticatedUser")
            .cookie(response.getCookies()))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header().exists(HttpHeaders.AUTHORIZATION))
            .andExpect(jsonPath("$.id_token", is(notNullValue())));

        String sessionId = "spring:session:index:org.springframework.session.FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME:master@gmail.com";
        Set keys = redisOperations.keys(sessionId);
        log.debug("Keys:before logout: {}", keys);
        assertThat(keys).isNotNull();
        assertThat(keys.size()).isEqualTo(1);

        mockMvc.perform(get("/api/logout")
            .cookie(response.getCookies()))
            .andExpect(status().isOk())
            .andExpect(cookie().value("SESSIONID", is(nullValue())));

        mockMvc.perform(get("/api/authenticatedUser")
            .cookie(response.getCookies()))
            .andExpect(status().is4xxClientError());

        keys = redisOperations.keys(sessionId);
        log.debug("Keys after logout: {}", keys);
        assertThat(keys).isNullOrEmpty();
    }

    @Test
    @DisplayName("Test - When Cal POST - /api/authenticate with default system user should return 401 - Unauthorized")
    public void shouldReturnUnauthorizedWhenCallApiWithDefaultSystemUser() throws Exception {
        LinkedMultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", DefaultUsers.SYSTEM_DEFAULT.getValue());
        formData.add("password", "noPassword");
        formData.add("rememberMe", "false");
        mockMvc.perform(post("/api/authenticate")
            .params(formData))
            .andDo(print())
            .andExpect(status().is4xxClientError())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header().doesNotExist(HttpHeaders.AUTHORIZATION))
            .andExpect(jsonPath("$.message", containsString("User is disabled")));
    }
}
