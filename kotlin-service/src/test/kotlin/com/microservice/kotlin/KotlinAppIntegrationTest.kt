package com.microservice.kotlin

import com.jayway.jsonpath.JsonPath.read
import com.microservice.jwt.common.TokenProvider
import com.microservice.kotlin.model.Task
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

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [KotlinApp::class], properties = ["configuration.swagger=false"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KotlinAppIntegrationTest(@Autowired val restTemplate: TestRestTemplate,
                               @Autowired val tokenProvider: TokenProvider) {

    @Test
    @DisplayName("When Calling POST - /api/tasks should create task response 201 - Created")
    fun shouldCreateAndDeleteTaskWhenCallingApi() {
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("dummy_user", null, listOf(SimpleGrantedAuthority("ROLE_TASK_CREATE"), SimpleGrantedAuthority("ROLE_TASK_DELETE")))

        val task = Task(name = "New Task")

        val headers = HttpHeaders()
        headers.add("authorization", "Bearer " + tokenProvider.createToken(usernamePasswordAuthenticationToken, "User", false))
        var responseEntity = restTemplate.exchange("/api/tasks", HttpMethod.POST, HttpEntity(task, headers), String::class.java)

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(read<String>(responseEntity.body, "$.id")).isNotEmpty()
        assertThat(read<String>(responseEntity.body, "$.name")).isEqualTo("New Task")
        assertThat(read<String>(responseEntity.body, "$.createdByUser")).isEqualTo("dummy_user")
        assertThat(read<Object>(responseEntity.body, "$.createdDate")).isNotNull

        responseEntity = restTemplate.exchange("/api/tasks/{id}", HttpMethod.DELETE, HttpEntity(null, headers), String::class.java, read<String>(responseEntity.body, "$.id"));

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    @DisplayName("When Calling GET - /api/tasks should return empty list and response 200 - OK")
    fun shouldFilterListByCreatedByUserWhenCallingApi() {
        var headers = HttpHeaders()
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("user", null, listOf(SimpleGrantedAuthority("ROLE_TASK_READ")))
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.createToken(usernamePasswordAuthenticationToken, "User", false))
        var responseEntity = restTemplate.exchange("/api/tasks", HttpMethod.GET, HttpEntity(null, headers), String::class.java)

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(read<List<String>>(responseEntity.body, "$")).isEmpty()

        headers = HttpHeaders()
        headers.add("authorization", "Bearer " + tokenProvider.createToken(usernamePasswordAuthenticationToken, "User", false))
        responseEntity = restTemplate.exchange("/api/tasks", HttpMethod.GET, HttpEntity(null, headers), String::class.java)

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(read<List<String>>(responseEntity.body, "$")).isEmpty()
    }

    @Test
    @DisplayName("When Calling GET - /api/tasks should return list of tasks and response 200 - OK")
    fun shouldReturnListOfTasksWhenCallingApi() {
        val headers = HttpHeaders()
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("user", null, listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.createToken(usernamePasswordAuthenticationToken, "Administrator", false))
        val responseEntity = restTemplate.exchange("/api/tasks", HttpMethod.GET, HttpEntity(null, headers), String::class.java)

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(read<List<String>>(responseEntity.body, "$..id")).hasSize(3)
        assertThat(read<List<String>>(responseEntity.body, "$..createdByUser").distinct()).containsExactly("default@admin.com")
    }

    @Test
    @DisplayName("When Calling GET - /api/tasks with different role should response 403 - Forbidden")
    fun shouldResponseForbiddenWhenRoleIsNotAppropriatedToListOfTask() {
        val headers = HttpHeaders()
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("user", null, listOf(SimpleGrantedAuthority("TASK_DELETE")))
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.createToken(usernamePasswordAuthenticationToken, "Administrator", false))
        val responseEntity = restTemplate.exchange("/api/tasks", HttpMethod.GET, HttpEntity(null, headers), String::class.java)

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

}
