package com.microservice.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.authentication.common.model.Authentication;
import com.microservice.authentication.common.model.Authority;
import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import com.microservice.authentication.dto.JwtTokenDto;
import com.microservice.web.common.util.constants.DefaultUsers;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import redis.embedded.RedisServer;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {SharedAuthenticationServiceApplicationIntegrationTest.EmbeddedRedisTestConfiguration.class, AuthenticationServiceApplication.class},
    properties = {"configuration.swagger=false",
        "logging.level.com.microservice=debug",
        "spring.redis.port=6370"})
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

    @Autowired @Qualifier("redisTemplate")
    RedisOperations redisOperations;

    @Configuration
    public static class EmbeddedRedisTestConfiguration {

        private RedisServer redisServer;

        @Value("${spring.redis.port}")
        private int redisPort;

        @PostConstruct
        public void startRedis() {
            this.redisServer = RedisServer.builder()
                .port(redisPort)
                .setting("maxmemory 128M") //maxheap 128M
                .build();
            log.debug("RedisServer: {}\tredisPort: {}", redisServer, redisPort);
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

    interface AuthenticationRepository extends AuthenticationCommonRepository, CrudRepository<Authentication, String> {
    }

    @Configuration
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

        @Bean
        EurekaClient eurekaClient() {
            EurekaClient eurekaClient = mock(EurekaClient.class);
            Applications applications = mock(Applications.class);
            Application application = mock(Application.class);
            when(application.getInstances()).thenReturn(Arrays.asList(InstanceInfo.Builder
                .newBuilder()
                .setIPAddr("127.0.0.1")
                .setPort(8080)
                .setAppName("mock-service")
                .build()));
            when(applications.getRegisteredApplications()).thenReturn(Arrays.asList(application));
            when(eurekaClient.getApplications()).thenReturn(applications);
            return eurekaClient;
        }

        private List<Authority> permissions(String ... permissions) {
            return Stream.of(permissions)
                .map(Authority::new)
                .collect(Collectors.toList());
        }
    }

    @Test
    @DisplayName("Test - When Calling POST - /oauth/token should return token and response 200 - OK")
    public void shouldReturnTokenWhenCallingApi() throws Exception {
        LinkedMultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", "client");
        formData.add("client_secret", "secret");
        formData.add("username", "master@gmail.com");
        formData.add("password", "password123");
        formData.add("grant_type", "password");
        formData.add("scope", "read");
        OAuth2AccessToken token = objectMapper.readValue(mockMvc.perform(post("/oauth/token")
            .params(formData))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.access_token", is(notNullValue())))
            .andExpect(jsonPath("$.token_type", is(notNullValue())))
            .andReturn()
            .getResponse()
            .getContentAsString(), OAuth2AccessToken.class);

        assertThat(token).isNotNull();
        assertThat(token.getValue()).isNotEmpty();

        TokenStore tokenStore = context.getBean(TokenStore.class);
        OAuth2AccessToken oAuth2AccessToken = tokenStore.readAccessToken(token.getValue());
        assertThat(oAuth2AccessToken.getExpiration()).isAfterOrEqualsTo(Date.from(ZonedDateTime.now().plusMinutes(30).toInstant()));
    }

    @Test
    @DisplayName("Test - When Calling POST - /login should be authenticated and response 200 - OK")
    public void shouldUserBeAuthenticatedWhenCallingApi() throws Exception {
        LinkedMultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", "master@gmail.com");
        formData.add("password", "password123");
        MockHttpServletResponse response = mockMvc.perform(post("/login")
            .params(formData)
            .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(cookie().value("SESSIONID", is(notNullValue())))
            .andReturn()
            .getResponse();

        assertThat(response.getCookies()).isNotNull();

        mockMvc.perform(get("/api/authenticatedUser")
            .with(csrf())
            .cookie(response.getCookies()))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header().exists(HttpHeaders.AUTHORIZATION))
            .andExpect(jsonPath("$.id_token", is(notNullValue())));
/*

        Set keys = redisOperations.keys("*");
        assertThat(keys).isNotNull();
        assertThat(keys.size()).isGreaterThan(0);
*/
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/login should display page and response 200 - OK")
    public void shouldDisplayLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/logout should response 200 - OK")
    public void shouldWorkLogoutUrl() throws Exception {
        mockMvc.perform(get("/api/logout"))
//            .andDo(print())
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Test - When Calling POST - /api/authenticatedUser should be authenticated and response 200 - OK")
    public void shouldWorkLoginAndLogout() throws Exception {
        LinkedMultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", "master@gmail.com");
        formData.add("password", "password123");
        formData.add("rememberMe", "false");

        MvcResult mvcResult = mockMvc.perform(post("/api/authenticate")
            .params(formData)
            .with(csrf()))
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
            .with(csrf())
            .cookie(response.getCookies()))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header().exists(HttpHeaders.AUTHORIZATION))
            .andExpect(jsonPath("$.id_token", is(notNullValue())));

/*
        String sessionId = "spring:session:index:org.springframework.session.FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME:master@gmail.com";
        Set keys = redisOperations.keys(sessionId);
        log.debug("Keys:before logout: {}", keys);
        assertThat(keys).isNotNull();
        assertThat(keys.size()).isEqualTo(1);
*/

        mockMvc.perform(get("/api/logout")
            .cookie(response.getCookies()))
            .andExpect(status().isOk())
            .andExpect(cookie().value("SESSIONID", is(nullValue())));

        mockMvc.perform(get("/api/authenticatedUser")
            .with(csrf())
            .cookie(response.getCookies()))
            .andExpect(status().is4xxClientError());

/*
        keys = redisOperations.keys(sessionId);
        log.debug("Keys after logout: {}", keys);
        assertThat(keys).isNullOrEmpty();
*/
    }


    @Test
    @DisplayName("Test - When Calling GET - /oauth/authenticatedUser without jwt should return 401 - Unauthorized")
    public void shouldReturnUnauthorizedWhenCallingApiWithoutAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/authenticatedUser")
            .with(csrf()))
            .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Test - When Calling POST - /oauth/token with default system user should return 401 - Unauthorized")
    public void shouldReturnUnauthorizedWhenCallingApiWithDefaultSystemUser() throws Exception {
        LinkedMultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", "client");
        formData.add("client_secret", "secret");
        formData.add("username", DefaultUsers.SYSTEM_DEFAULT.getValue());
        formData.add("password", "noPassword");
        formData.add("grant_type", "password");
        formData.add("scope", "read");

        mockMvc.perform(post("/oauth/token")
            .params(formData))
            .andExpect(status().is4xxClientError())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header().doesNotExist(HttpHeaders.AUTHORIZATION))
            .andExpect(jsonPath("$.error_description", containsString("User is disabled")));
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
}
