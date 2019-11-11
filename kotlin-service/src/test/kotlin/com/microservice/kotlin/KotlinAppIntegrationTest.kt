package com.microservice.kotlin

import com.jayway.jsonpath.JsonPath
import com.microservice.jwt.common.TokenProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.Instant
import java.time.format.DateTimeFormatter

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [KotlinApp::class], properties = ["configuration.swagger=false", "debug=true", "logging.level.org.springframework.security=debug"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KotlinAppIntegrationTest(@Autowired val restTemplate: TestRestTemplate,
                               @Autowired val tokenProvider: TokenProvider) {

    @Test
    @DisplayName("When Calling GET - /api/tasks should return empty list and response 200 - OK")
    fun shouldFilterListByCreatedByUserWhenCallingApi() {
        val headers = HttpHeaders()
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("user", null, listOf(SimpleGrantedAuthority("ROLE_TASK_READ")))
        headers.add(HttpHeaders.AUTHORIZATION, tokenProvider.createToken(usernamePasswordAuthenticationToken, "User", false))
        val responseEntity = restTemplate.exchange("/api/tasks", HttpMethod.GET, HttpEntity(null, headers), String::class.java)

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(JsonPath.read<List<String>>(responseEntity.body, "$")).isEmpty()
    }

    @Test
    @DisplayName("When Calling GET - /api/tasks should return list of tasks and response 200 - OK")
    fun shouldReturnListOfTasksWhenCallingApi() {
        val headers = HttpHeaders()
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("user", null, listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
        headers.add(HttpHeaders.AUTHORIZATION, tokenProvider.createToken(usernamePasswordAuthenticationToken, "Administrator", false))
        val responseEntity = restTemplate.exchange("/api/tasks", HttpMethod.GET, HttpEntity(null, headers), String::class.java)

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(JsonPath.read<List<String>>(responseEntity.body, "$..id")).hasSize(3)
        assertThat(JsonPath.read<List<String>>(responseEntity.body, "$..createdByUser").distinct()).containsExactly("default@admin.com")
        assertThat(JsonPath.read<String>(responseEntity.body, "$.createdDate[0]")).startsWith(DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
    }

    @Test
    @DisplayName("When Calling GET - /api/tasks with different role should response 403 - Forbidden")
    fun shouldResponseForbiddenWhenRoleIsNotAppropriatedToListOfTask() {
        val headers = HttpHeaders()
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("user", null, listOf(SimpleGrantedAuthority("TASK_DELETE")))
        headers.add(HttpHeaders.AUTHORIZATION, tokenProvider.createToken(usernamePasswordAuthenticationToken, "Administrator", false))
        val responseEntity = restTemplate.exchange("/api/tasks", HttpMethod.GET, HttpEntity(null, headers), String::class.java)

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

}
