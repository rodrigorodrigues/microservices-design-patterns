package com.microservice.kotlin

import com.jayway.jsonpath.JsonPath
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
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [KotlinApp::class], properties = ["configuration.swagger=false", "debug=true", "logging.level.org.springframework.security=debug"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KotlinAppIntegrationTest(@Autowired val restTemplate: TestRestTemplate) {

    @Test
    @DisplayName("Test - When Calling GET - /api/tasks should return list of tasks and response 200 - OK")
    @WithMockUser(roles = ["TASK_READ"], username = "rodrigo")
    fun shouldReturnListOfTasksWhenCallingApi() {
        val headers = HttpHeaders()
        headers.add(HttpHeaders.AUTHORIZATION, "Mock JWT")
        val responseEntity = restTemplate.exchange("/api/tasks", HttpMethod.GET, HttpEntity(null, headers), String::class.java)

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(JsonPath.read<List<String>>(responseEntity.body, "$..id")).hasSize(3)
    }

/*
    @Test
    @DisplayName("Test - When Calling GET - /api/tasks without user should response 403 - Forbidden")
    fun shouldResponseForbiddenWhenCallingApiWithoutUser() {
        restTemplate.perform(get("/api/tasks"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().is4xxClientError)
    }
*/
}
