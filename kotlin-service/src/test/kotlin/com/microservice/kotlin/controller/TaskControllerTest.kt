package com.microservice.kotlin.controller

import com.microservice.authentication.autoconfigure.AuthenticationProperties
import com.microservice.kotlin.JwtTokenUtil
import com.microservice.kotlin.config.SpringSecurityConfiguration
import com.microservice.kotlin.dto.TaskDto
import com.microservice.kotlin.mapper.TaskMapper
import com.microservice.kotlin.repository.TaskRepository
import com.microservice.kotlin.service.TaskService
import com.microservice.web.autoconfigure.WebCommonAutoConfiguration
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import tools.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.spec.SecretKeySpec

@WebMvcTest(properties = ["configuration.mongo=false"], controllers = [TaskController::class])
@Import(*[ WebCommonAutoConfiguration::class, SpringSecurityConfiguration::class])
@EnableConfigurationProperties(value = [AuthenticationProperties::class])
internal class TaskControllerTest(@Autowired val client: MockMvc,
                                  @Autowired val objectMapper: ObjectMapper,
                                  @Autowired val authenticationProperties: AuthenticationProperties) {
    @MockitoBean
    lateinit var taskService: TaskService

    var jwtTokenUtil = JwtTokenUtil(authenticationProperties)

    @TestConfiguration
    internal class MockConfiguration {
        @Bean
        fun taskRepository(): TaskRepository {
            return mock(TaskRepository::class.java)
        }

        @Bean
        fun jwtDecoder(properties: AuthenticationProperties): JwtDecoder? {
            val secretKeySpec = SecretKeySpec(properties.jwt.keyValue.toByteArray(StandardCharsets.UTF_8), "HS256")
            return NimbusJwtDecoder.withSecretKey(secretKeySpec).build()
        }

        @Bean
        fun taskMapper(): TaskMapper {
            return mock(TaskMapper::class.java)
        }
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/tasks should return empty list and response 200 - OK")
    fun shouldReturnEmptyList() {
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("admin", null, listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
        `when`(taskService.findAll(any(), any()))
            .thenReturn(PageableExecutionUtils.getPage(listOf(), Pageable.unpaged()) { 0 })

        client.perform(get("/api/tasks")
            .header(AUTHORIZATION, jwtTokenUtil.createToken(usernamePasswordAuthenticationToken)))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andExpect(jsonPath("$.content").isEmpty)
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/tasks should return list of tasks and response 200 - OK")
    fun shouldReturnListOfAllTasksWhenCallingApi() {
        `when`(taskService.findAll(any(), any())).thenReturn(PageableExecutionUtils.getPage(listOf(
            TaskDto(UUID.randomUUID().toString(), name = "Test", createdByUser = "admin"),
            TaskDto(UUID.randomUUID().toString(), name = "Test 2", createdByUser = "anonymous")
        ), Pageable.unpaged()
        ) { 2 })

        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("anyone", null, listOf(SimpleGrantedAuthority("ROLE_ADMIN")))

        client.perform(get("/api/tasks")
            .header(AUTHORIZATION, jwtTokenUtil.createToken(usernamePasswordAuthenticationToken)))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andExpect(jsonPath("$..name").value(containsInAnyOrder("Test", "Test 2")))
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/tasks without user should response 403 - Forbidden")
    fun shouldResponseForbiddenWhenCallingApiWithoutUser() {
        client.perform(get("/api/tasks")
            .header(AUTHORIZATION, "Bearer Mock JWT"))
            .andExpect(status().is4xxClientError)
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/tasks/{id} with valid user should response 200 - OK")
    fun testFindById() {
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("test", null, listOf(SimpleGrantedAuthority("ROLE_TASK_READ")))

        `when`(taskService.findById(anyString())).thenReturn(TaskDto(UUID.randomUUID().toString(), name = "Test", createdByUser = "test"))

        client.perform(get("/api/tasks/{id}", 999)
            .header(AUTHORIZATION, jwtTokenUtil.createToken(usernamePasswordAuthenticationToken)))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andExpect(jsonPath("$.name").value("Test"))
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/tasks/{id} with user that is not the same as created task should response 403 - Forbidden")
    fun testFindByIdShouldResponseForbidden() {
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("admin", null, listOf(SimpleGrantedAuthority("TASK_READ")))

        `when`(taskService.findById(anyString())).thenReturn(TaskDto(UUID.randomUUID().toString(), name = "Test", createdByUser = "test"))

        client.perform(get("/api/tasks/{id}", 999)
            .header(AUTHORIZATION, jwtTokenUtil.createToken(usernamePasswordAuthenticationToken)))
            .andExpect(status().isForbidden)
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/tasks/{id} with invalid role should response 403 - Forbidden")
    fun testFindByIdWithInvalidRoleShouldReturnForbidden() {
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("test", null, listOf(SimpleGrantedAuthority("TASK_CREATE")))

        `when`(taskService.findById(anyString())).thenReturn(TaskDto(UUID.randomUUID().toString(), name = "Test"))

        client.perform(get("/api/tasks/{id}", 999)
            .header(AUTHORIZATION, jwtTokenUtil.createToken(usernamePasswordAuthenticationToken)))
            .andExpect(status().is4xxClientError)
    }

    @Test
    @DisplayName("Test - When Calling POST - /api/tasks/ should create task and response 201 - Created")
    fun testCreate() {
        `when`(taskService.save(any())).thenReturn(TaskDto(UUID.randomUUID().toString(), name = "New Task"))
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("admin", null, listOf(SimpleGrantedAuthority("ROLE_ADMIN")))

        client.perform(post("/api/tasks")
            .header(AUTHORIZATION, jwtTokenUtil.createToken(usernamePasswordAuthenticationToken))
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(TaskDto(id = "", name = "New Task"))))
            .andExpect(status().isCreated)
            .andExpect(header().exists(LOCATION))
            .andExpect(jsonPath("$.createdByUser").value("admin"))
            .andExpect(jsonPath("$.name").value("New Task"))
            .andExpect(jsonPath("$.id").isNotEmpty)

        verify(taskService).save(any())
    }

    @Test
    @DisplayName("Test - When Calling POST - /api/tasks/ with id should update task and response 200 - OK")
    fun testCreateShouldUpdateWhenIdExists() {
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("admin", null, listOf(SimpleGrantedAuthority("ROLE_ADMIN")))

        val task = TaskDto("999", name = "Test", createdByUser = "test")
        `when`(taskService.findById(anyString())).thenReturn(task)

        client.perform(post("/api/tasks")
            .header(AUTHORIZATION, jwtTokenUtil.createToken(usernamePasswordAuthenticationToken))
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(TaskDto(id = "999", name = "New Task"))))
            .andExpect(status().isOk)
            .andExpect(header().doesNotExist(LOCATION))

        verify(taskService).save(any())
    }

    @Test
    @DisplayName("Test - When Calling PUT - /api/tasks/{id} should update task and response 200 - OK")
    fun testUpdate() {
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("test", null, listOf(SimpleGrantedAuthority("ROLE_TASK_SAVE")))

        val task = TaskDto(UUID.randomUUID().toString(), name = "Test", createdByUser = "test")
        `when`(taskService.findById(any())).thenReturn(task)
        `when`(taskService.save(any())).thenReturn(task.copy(name = "Updated Task"))

        client.perform(put("/api/tasks/{id}", 999)
            .header(AUTHORIZATION, jwtTokenUtil.createToken(usernamePasswordAuthenticationToken))
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(TaskDto(id = task.id, name = "Updated Task", createdByUser = "test"))))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andExpect(jsonPath("$.name").value("Updated Task"))

        verify(taskService).save(any())
    }

    @Test
    @DisplayName("Test - When Calling PUT - /api/tasks/{id} with different createdByUser should response 403 - Forbidden")
    fun testUpdateWithDifferentCreatedByUserShouldResponseForbidden() {
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("admin", null, listOf(SimpleGrantedAuthority("TASK_SAVE")))

        client.perform(put("/api/tasks/{id}", 999)
            .header(AUTHORIZATION, jwtTokenUtil.createToken(usernamePasswordAuthenticationToken))
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(TaskDto(id = UUID.randomUUID().toString(), name = "Updated Task", createdByUser = "test"))))
            .andExpect(status().is4xxClientError)

        verify(taskService, never()).save(any())
    }

    @Test
    @DisplayName("Test - When Calling DELETE - /api/tasks/{id} should remove task and response 200 - OK")
    fun testDelete() {
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("newUser", null, listOf(SimpleGrantedAuthority("ROLE_TASK_DELETE")))

        val id = UUID.randomUUID().toString()
        val task = TaskDto(id, name = "Test", createdByUser = "newUser")
        `when`(taskService.findById(any())).thenReturn(task)

        client.perform(delete("/api/tasks/{id}", id)
            .header(AUTHORIZATION, jwtTokenUtil.createToken(usernamePasswordAuthenticationToken)))
            .andExpect(status().isOk)

        verify(taskService).deleteById(id)
    }

    @Test
    @DisplayName("Test - When Calling DELETE - /api/tasks/{id} with different user should response 403 - Forbidden")
    fun testDeleteWithDifferentUserShouldResponseForbidden() {
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("admin", null, listOf(SimpleGrantedAuthority("TASK_DELETE")))

        val id = UUID.randomUUID().toString()
        val task = TaskDto(id, name = "Test", createdByUser = "newUser")
        `when`(taskService.findById(anyString())).thenReturn(task)

        client.perform(delete("/api/tasks/{id}", id)
            .header(AUTHORIZATION, jwtTokenUtil.createToken(usernamePasswordAuthenticationToken)))
            .andExpect(status().is4xxClientError)

        verify(taskService, never()).deleteById(id)
    }

    @Test
    @DisplayName("Test - When Calling DELETE - /api/tasks/{id} with admin user should delete task and response 200 - OK")
    fun testDeleteWithRoleAdminShouldResponseOk() {
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("testUser", null, listOf(SimpleGrantedAuthority("ROLE_ADMIN")))

        val id = UUID.randomUUID().toString()
        val task = TaskDto(id, name = "Test", createdByUser = "newUser")
        `when`(taskService.findById(any())).thenReturn(task)

        client.perform(delete("/api/tasks/{id}", id)
            .header(AUTHORIZATION, jwtTokenUtil.createToken(usernamePasswordAuthenticationToken)))
            .andExpect(status().isOk)

        verify(taskService).deleteById(id)
    }
}
