package com.microservice.kotlin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.microservice.jwt.common.TokenProvider
import com.microservice.kotlin.config.CustomDefaultErrorAttributes
import com.microservice.kotlin.model.Task
import com.microservice.kotlin.repository.TaskRepository
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.*

@Configuration
internal class MockCustomErrorAttributes {
    @Bean
    fun customDefaultErrorAttributes(): CustomDefaultErrorAttributes = CustomDefaultErrorAttributes()
}

@ExtendWith(SpringExtension::class)
@WebMvcTest
@AutoConfigureMockMvc
@Import(MockCustomErrorAttributes::class)
internal class TaskControllerTest(@Autowired val client: MockMvc,
                                  @Autowired val objectMapper: ObjectMapper
                                  ) {
    @MockBean lateinit var tokenProvider : TokenProvider
    @MockBean lateinit var taskRepository: TaskRepository

    @BeforeEach
    fun setup() {
        `when`(tokenProvider.validateToken(ArgumentMatchers.anyString())).thenReturn(true)
        `when`(taskRepository.findAll()).thenReturn(listOf(
            Task(UUID.randomUUID().toString(), name = "Test", createdByUser = "rodrigo"),
            Task(UUID.randomUUID().toString(), name = "Test 2", createdByUser = "gustavo")
        ))
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/tasks should return empty list and response 200 - OK")
    @WithMockUser(roles = ["TASK_READ"], username = "rodrigo")
    fun shouldReturnEmptyList() {
        `when`(taskRepository.findAll()).thenReturn(listOf())

        client.perform(get("/api/tasks")
            .header(AUTHORIZATION, "Bearer Mock JWT"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andExpect(jsonPath("$").isEmpty)
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/tasks should return filtered list of tasks and response 200 - OK")
    @WithMockUser(roles = ["TASK_READ"], username = "rodrigo")
    fun shouldReturnListOfTasksAndFilterByUserWhenCallingApi() {
        client.perform(get("/api/tasks")
            .header(AUTHORIZATION, "Bearer Mock JWT"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andExpect(jsonPath("$..name").value(containsInAnyOrder("Test")))
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/tasks should return list of tasks and response 200 - OK")
    @WithMockUser(roles = ["ADMIN"], username = "anyone")
    fun shouldReturnListOfAllTasksWhenCallingApi() {
        client.perform(get("/api/tasks")
            .header(AUTHORIZATION, "Bearer Mock JWT"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andExpect(jsonPath("$..name").value(containsInAnyOrder("Test", "Test 2")))
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/tasks without user should response 403 - Forbidden")
    fun shouldResponseForbiddenWhenCallingApiWithoutUser() {
        client.perform(get("/api/tasks")
            .header(AUTHORIZATION, "Bearer Mock JWT"))
            .andDo(print())
            .andExpect(status().is4xxClientError)
    }

    @Test
    @WithMockUser(roles = ["TASK_READ"], username = "test")
    @DisplayName("Test - When Calling GET - /api/tasks/{id} with valid user should response 200 - OK")
    fun testFindById() {
        `when`(taskRepository.findById(ArgumentMatchers.anyString())).thenReturn(Optional.of(Task(UUID.randomUUID().toString(), name = "Test", createdByUser = "test")))

        client.perform(get("/api/tasks/{id}", 999)
            .header(AUTHORIZATION, "Bearer Mock JWT"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andExpect(jsonPath("$.name").value("Test"))
    }

    @Test
    @WithMockUser(roles = ["TASK_READ"], username = "rodrigo")
    @DisplayName("Test - When Calling GET - /api/tasks/{id} with user that is not the same as created task should response 403 - Forbidden")
    fun testFindByIdShouldResponseForbidden() {
        `when`(taskRepository.findById(ArgumentMatchers.anyString())).thenReturn(Optional.of(Task(UUID.randomUUID().toString(), name = "Test", createdByUser = "test")))

        client.perform(get("/api/tasks/{id}", 999)
            .header(AUTHORIZATION, "Bearer Mock JWT"))
            .andDo(print())
            .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(roles = ["TASK_CREATE"], username = "test")
    @DisplayName("Test - When Calling GET - /api/tasks/{id} with invalid role should response 403 - Forbidden")
    fun testFindByIdWithInvalidRoleShouldReturnForbidden() {
        `when`(taskRepository.findById(ArgumentMatchers.anyString())).thenReturn(Optional.of(Task(UUID.randomUUID().toString(), name = "Test")))

        client.perform(get("/api/tasks/{id}", 999)
            .header(AUTHORIZATION, "Bearer Mock JWT"))
            .andDo(print())
            .andExpect(status().is4xxClientError)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"], username = "admin")
    @DisplayName("Test - When Calling POST - /api/tasks/ should create task and response 201 - Created")
    fun testCreate() {
        client.perform(post("/api/tasks")
            .header(AUTHORIZATION, "Bearer Mock JWT")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Task(id = "", name = "New Task"))))
            .andDo(print())
            .andExpect(status().isCreated)
            .andExpect(header().exists(LOCATION))
            .andExpect(jsonPath("$.createdByUser").value("admin"))
            .andExpect(jsonPath("$.name").value("New Task"))

        verify(taskRepository).save(ArgumentMatchers.any(Task::class.java))
    }

    @Test
    @WithMockUser(roles = ["TASK_SAVE"], username = "test")
    @DisplayName("Test - When Calling PUT - /api/tasks/{id} should update task and response 200 - OK")
    fun testUpdate() {
        val task = Task(UUID.randomUUID().toString(), name = "Test", createdByUser = "test")
        `when`(taskRepository.findById(ArgumentMatchers.anyString())).thenReturn(Optional.of(task))
        `when`(taskRepository.save(any(Task::class.java))).thenReturn(task.copy(name = "Updated Task"))

        client.perform(put("/api/tasks/{id}", 999)
            .header(AUTHORIZATION, "Bearer Mock JWT")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Task(id = task.id, name = "Updated Task", createdByUser = "test"))))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andExpect(jsonPath("$.name").value("Updated Task"))

        verify(taskRepository).save(ArgumentMatchers.any(Task::class.java))
    }

    @Test
    @WithMockUser(roles = ["TASK_SAVE"], username = "admin")
    @DisplayName("Test - When Calling PUT - /api/tasks/{id} with different createdByUser should response 403 - Forbidden")
    fun testUpdateWithDifferentCreatedByUserShouldResponseForbidden() {
        val task = Task(UUID.randomUUID().toString(), name = "Test", createdByUser = "admin")
        `when`(taskRepository.findById(ArgumentMatchers.anyString())).thenReturn(Optional.of(task))

        client.perform(put("/api/tasks/{id}", 999)
            .header(AUTHORIZATION, "Bearer Mock JWT")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Task(id = task.id, name = "Updated Task", createdByUser = "test"))))
            .andDo(print())
            .andExpect(status().is4xxClientError)

        verify(taskRepository, never()).save(ArgumentMatchers.any(Task::class.java))
    }

    @Test
    @WithMockUser(roles = ["TASK_DELETE"], username = "newUser")
    @DisplayName("Test - When Calling DELETE - /api/tasks/{id} should remove task and response 200 - OK")
    fun testDelete() {
        val id = UUID.randomUUID().toString()
        val task = Task(id, name = "Test", createdByUser = "newUser")
        `when`(taskRepository.findById(ArgumentMatchers.anyString())).thenReturn(Optional.of(task))

        client.perform(delete("/api/tasks/{id}", id)
            .header(AUTHORIZATION, "Bearer Mock JWT"))
            .andExpect(status().isOk)

        verify(taskRepository).deleteById(id)
    }

    @Test
    @WithMockUser(roles = ["TASK_DELETE"], username = "admin")
    @DisplayName("Test - When Calling DELETE - /api/tasks/{id} with different user should response 403 - Forbidden")
    fun testDeleteWithDifferentUserShouldResponseForbidden() {
        val id = UUID.randomUUID().toString()
        val task = Task(id, name = "Test", createdByUser = "newUser")
        `when`(taskRepository.findById(ArgumentMatchers.anyString())).thenReturn(Optional.of(task))

        client.perform(delete("/api/tasks/{id}", id)
            .header(AUTHORIZATION, "Bearer Mock JWT"))
            .andExpect(status().is4xxClientError)

        verify(taskRepository, never()).deleteById(id)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"], username = "testUser")
    @DisplayName("Test - When Calling DELETE - /api/tasks/{id} with admin user should delete task and response 200 - OK")
    fun testDeleteWithRoleAdminShouldResponseOk() {
        val id = UUID.randomUUID().toString()
        val task = Task(id, name = "Test", createdByUser = "newUser")
        `when`(taskRepository.findById(ArgumentMatchers.anyString())).thenReturn(Optional.of(task))

        client.perform(delete("/api/tasks/{id}", id)
            .header(AUTHORIZATION, "Bearer Mock JWT"))
            .andExpect(status().isOk)

        verify(taskRepository).deleteById(id)
    }
}
