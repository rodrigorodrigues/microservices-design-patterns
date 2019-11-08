package com.microservice.kotlin

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [KotlinApp::class], properties = ["configuration.swagger=false", "debug=true", "logging.level.org.springframework.security=debug"])
@AutoConfigureMockMvc
class KotlinAppIntegrationTest(@Autowired val client: MockMvc) {

    @Test
    @DisplayName("Test - When Calling GET - /api/tasks should return list of tasks and response 200 - OK")
    @WithMockUser(roles = ["TASK_READ"], username = "rodrigo")
    fun shouldReturnListOfTasksWhenCallingApi() {
        client.perform(get("/api/tasks"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$..id").isArray)
 //           .andExpect(jsonPath("$.createdBy[0]").value("rodrigo"))
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/tasks without user should response 403 - Forbidden")
    fun shouldResponseForbiddenWhenCallingApiWithoutUser() {
        client.perform(get("/api/tasks"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().is4xxClientError)
    }
}
