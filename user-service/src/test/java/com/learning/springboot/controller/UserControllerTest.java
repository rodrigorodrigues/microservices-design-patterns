package com.learning.springboot.controller;

import com.learning.springboot.config.SpringSecurityConfiguration;
import com.learning.springboot.config.jwt.TokenProvider;
import com.learning.springboot.service.UserService;
import com.learning.springboot.util.HandleResponseError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

@ExtendWith(SpringExtension.class)
@WebFluxTest(properties = {
        "configuration.initialLoad=false",
        "configuration.mongo=false",
        "debug=true",
        "logging.level.org.springframework=debug"},
        controllers = UserController.class, excludeAutoConfiguration = MongoReactiveAutoConfiguration.class)
@Import({SpringSecurityConfiguration.class, HandleResponseError.class, ErrorWebFluxAutoConfiguration.class})
class UserControllerTest {

    @Autowired
    WebTestClient client;

    @MockBean
    UserService userService;

    @MockBean
    TokenProvider tokenProvider;

    @Test
    @DisplayName("Test - When Cal GET - /api/users without valid authorization the response should be 403 - Forbidden")
    @WithMockUser(roles = "INVALID_ROLE")
    public void whenCallFindAllShouldReturnForbiddenWhenDoesNotHavePermission() {
        client.get().uri("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .exchange()
                .expectStatus().isForbidden();
    }
}