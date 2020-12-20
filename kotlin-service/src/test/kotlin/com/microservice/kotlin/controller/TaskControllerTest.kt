package com.microservice.kotlin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.microservice.authentication.autoconfigure.AuthenticationCommonConfiguration
import com.microservice.authentication.resourceserver.config.ActuatorResourceServerConfiguration
import com.microservice.kotlin.JwtTokenUtil
import com.microservice.kotlin.KotlinApplicationIntegrationTest
import com.microservice.kotlin.model.Task
import com.microservice.kotlin.repository.TaskRepository
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import com.querydsl.core.types.Predicate
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.annotation.Import
import org.springframework.context.support.GenericApplicationContext
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.support.PageableExecutionUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.security.KeyPair
import java.security.interfaces.RSAPublicKey
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier

@WebMvcTest(properties = ["configuration.initialLoad=false", "configuration.mongo=false"], controllers = [TaskController::class], excludeAutoConfiguration = [MongoAutoConfiguration::class])
@ContextConfiguration(initializers = [KotlinApplicationIntegrationTest.GenerateKeyPairInitializer::class, TaskControllerTest.JwtDecoderInitializer::class])
@AutoConfigureWireMock(port = 0)
@Import(AuthenticationCommonConfiguration::class, ActuatorResourceServerConfiguration::class)
@EnableConfigurationProperties(ResourceServerProperties::class)
internal class TaskControllerTest(@Autowired val client: MockMvc,
                                  @Autowired val objectMapper: ObjectMapper,
                                  @Autowired val keyPair: KeyPair) {
    @MockBean lateinit var taskRepository: TaskRepository

    var runAtOnce = AtomicBoolean(true)

    var jwtTokenUtil = JwtTokenUtil(keyPair)

    internal class JwtDecoderInitializer : ApplicationContextInitializer<GenericApplicationContext> {
        override fun initialize(applicationContext: GenericApplicationContext) {
            applicationContext.registerBean(TaskRepository::class.java, Supplier { mock(TaskRepository::class.java) })
        }
    }

    @BeforeEach
    fun setup() {
        if (runAtOnce.getAndSet(false)) {
            val builder = RSAKey.Builder(keyPair!!.public as RSAPublicKey)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyID("test")
            val jwkSet = JWKSet(builder.build())
            val jsonPublicKey = jwkSet.toJSONObject().toJSONString()
            WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/.well-known/jwks.json"))
                .willReturn(WireMock.aResponse().withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).withBody(jsonPublicKey)))
        }

        `when`(taskRepository.findAll(any(Predicate::class.java), any(Pageable::class.java))).thenReturn(PageableExecutionUtils.getPage(listOf(
            Task(UUID.randomUUID().toString(), name = "Test", createdByUser = "admin"),
            Task(UUID.randomUUID().toString(), name = "Test 2", createdByUser = "anonymous")
        ), Pageable.unpaged(), { 2 }))
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/tasks should return empty list and response 200 - OK")
    fun shouldReturnEmptyList() {
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("admin", null, listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
        `when`(taskRepository.findAll(any(Predicate::class.java), any(Pageable::class.java))).thenReturn(PageableExecutionUtils.getPage(listOf(), Pageable.unpaged(), { 0 }))

        client.perform(get("/api/tasks")
            .header(AUTHORIZATION, jwtTokenUtil.createToken(usernamePasswordAuthenticationToken)))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andExpect(jsonPath("$.content").isEmpty)
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/tasks should return list of tasks and response 200 - OK")
    fun shouldReturnListOfAllTasksWhenCallingApi() {
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

        `when`(taskRepository.findById(ArgumentMatchers.anyString())).thenReturn(Optional.of(Task(UUID.randomUUID().toString(), name = "Test", createdByUser = "test")))

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

        `when`(taskRepository.findById(ArgumentMatchers.anyString())).thenReturn(Optional.of(Task(UUID.randomUUID().toString(), name = "Test", createdByUser = "test")))

        client.perform(get("/api/tasks/{id}", 999)
            .header(AUTHORIZATION, jwtTokenUtil.createToken(usernamePasswordAuthenticationToken)))
            .andExpect(status().isForbidden)
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/tasks/{id} with invalid role should response 403 - Forbidden")
    fun testFindByIdWithInvalidRoleShouldReturnForbidden() {
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("test", null, listOf(SimpleGrantedAuthority("TASK_CREATE")))

        `when`(taskRepository.findById(ArgumentMatchers.anyString())).thenReturn(Optional.of(Task(UUID.randomUUID().toString(), name = "Test")))

        client.perform(get("/api/tasks/{id}", 999)
            .header(AUTHORIZATION, jwtTokenUtil.createToken(usernamePasswordAuthenticationToken)))
            .andExpect(status().is4xxClientError)
    }

    @Test
    @DisplayName("Test - When Calling POST - /api/tasks/ should create task and response 201 - Created")
    fun testCreate() {
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("admin", null, listOf(SimpleGrantedAuthority("ROLE_ADMIN")))

        client.perform(post("/api/tasks")
            .header(AUTHORIZATION, jwtTokenUtil.createToken(usernamePasswordAuthenticationToken))
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Task(id = "", name = "New Task"))))
            .andExpect(status().isCreated)
            .andExpect(header().exists(LOCATION))
            .andExpect(jsonPath("$.createdByUser").value("admin"))
            .andExpect(jsonPath("$.name").value("New Task"))

        verify(taskRepository).save(ArgumentMatchers.any(Task::class.java))
    }

    @Test
    @DisplayName("Test - When Calling POST - /api/tasks/ with id should update task and response 200 - OK")
    fun testCreateShouldUpdateWhenIdExists() {
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("admin", null, listOf(SimpleGrantedAuthority("ROLE_ADMIN")))

        val task = Task("999", name = "Test", createdByUser = "test")
        `when`(taskRepository.findById(ArgumentMatchers.anyString())).thenReturn(Optional.of(task))

        client.perform(post("/api/tasks")
            .header(AUTHORIZATION, jwtTokenUtil.createToken(usernamePasswordAuthenticationToken))
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Task(id = "999", name = "New Task"))))
            .andExpect(status().isOk)
            .andExpect(header().doesNotExist(LOCATION))

        verify(taskRepository).save(ArgumentMatchers.any(Task::class.java))
    }

    @Test
    @DisplayName("Test - When Calling PUT - /api/tasks/{id} should update task and response 200 - OK")
    fun testUpdate() {
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("test", null, listOf(SimpleGrantedAuthority("ROLE_TASK_SAVE")))

        val task = Task(UUID.randomUUID().toString(), name = "Test", createdByUser = "test")
        `when`(taskRepository.findById(ArgumentMatchers.anyString())).thenReturn(Optional.of(task))
        `when`(taskRepository.save(any(Task::class.java))).thenReturn(task.copy(name = "Updated Task"))

        client.perform(put("/api/tasks/{id}", 999)
            .header(AUTHORIZATION, jwtTokenUtil.createToken(usernamePasswordAuthenticationToken))
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Task(id = task.id, name = "Updated Task", createdByUser = "test"))))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andExpect(jsonPath("$.name").value("Updated Task"))

        verify(taskRepository).save(ArgumentMatchers.any(Task::class.java))
    }

    @Test
    @DisplayName("Test - When Calling PUT - /api/tasks/{id} with different createdByUser should response 403 - Forbidden")
    fun testUpdateWithDifferentCreatedByUserShouldResponseForbidden() {
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("admin", null, listOf(SimpleGrantedAuthority("TASK_SAVE")))

        client.perform(put("/api/tasks/{id}", 999)
            .header(AUTHORIZATION, jwtTokenUtil.createToken(usernamePasswordAuthenticationToken))
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Task(id = UUID.randomUUID().toString(), name = "Updated Task", createdByUser = "test"))))
            .andExpect(status().is4xxClientError)

        verify(taskRepository, never()).save(ArgumentMatchers.any(Task::class.java))
    }

    @Test
    @DisplayName("Test - When Calling DELETE - /api/tasks/{id} should remove task and response 200 - OK")
    fun testDelete() {
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("newUser", null, listOf(SimpleGrantedAuthority("ROLE_TASK_DELETE")))

        val id = UUID.randomUUID().toString()
        val task = Task(id, name = "Test", createdByUser = "newUser")
        `when`(taskRepository.findById(ArgumentMatchers.anyString())).thenReturn(Optional.of(task))

        client.perform(delete("/api/tasks/{id}", id)
            .header(AUTHORIZATION, jwtTokenUtil.createToken(usernamePasswordAuthenticationToken)))
            .andExpect(status().isOk)

        verify(taskRepository).deleteById(id)
    }

    @Test
    @DisplayName("Test - When Calling DELETE - /api/tasks/{id} with different user should response 403 - Forbidden")
    fun testDeleteWithDifferentUserShouldResponseForbidden() {
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("admin", null, listOf(SimpleGrantedAuthority("TASK_DELETE")))

        val id = UUID.randomUUID().toString()
        val task = Task(id, name = "Test", createdByUser = "newUser")
        `when`(taskRepository.findById(ArgumentMatchers.anyString())).thenReturn(Optional.of(task))

        client.perform(delete("/api/tasks/{id}", id)
            .header(AUTHORIZATION, jwtTokenUtil.createToken(usernamePasswordAuthenticationToken)))
            .andExpect(status().is4xxClientError)

        verify(taskRepository, never()).deleteById(id)
    }

    @Test
    @DisplayName("Test - When Calling DELETE - /api/tasks/{id} with admin user should delete task and response 200 - OK")
    fun testDeleteWithRoleAdminShouldResponseOk() {
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("testUser", null, listOf(SimpleGrantedAuthority("ROLE_ADMIN")))

        val id = UUID.randomUUID().toString()
        val task = Task(id, name = "Test", createdByUser = "newUser")
        `when`(taskRepository.findById(ArgumentMatchers.anyString())).thenReturn(Optional.of(task))

        client.perform(delete("/api/tasks/{id}", id)
            .header(AUTHORIZATION, jwtTokenUtil.createToken(usernamePasswordAuthenticationToken)))
            .andExpect(status().isOk)

        verify(taskRepository).deleteById(id)
    }
}
