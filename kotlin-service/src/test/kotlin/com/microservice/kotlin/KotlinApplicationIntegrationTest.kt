package com.microservice.kotlin

import com.jayway.jsonpath.JsonPath.read
import com.microservice.authentication.autoconfigure.AuthenticationProperties
import com.microservice.kotlin.dto.TaskDto
import com.microservice.kotlin.model.Task
import com.microservice.kotlin.repository.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.boot.resttestclient.exchange
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.test.context.ContextConfiguration
import java.nio.charset.StandardCharsets
import java.security.interfaces.RSAPublicKey
import javax.crypto.spec.SecretKeySpec

@Import(TestcontainersConfiguration::class)
@SpringBootTest(classes = [KotlinApplication::class], properties = ["configuration.swagger=false"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = [KotlinApplicationIntegrationTest.PopulateDbConfiguration::class])
@AutoConfigureTestRestTemplate
class KotlinApplicationIntegrationTest(@Autowired val restTemplate: TestRestTemplate,
                                       @Autowired val authenticationProperties: AuthenticationProperties,
                                       @Autowired val taskRepository: TaskRepository) {

    var jwtTokenUtil = JwtTokenUtil(authenticationProperties)

    @TestConfiguration
    class PopulateDbConfiguration : ApplicationContextAware {
        private lateinit var applicationContext: ApplicationContext

        @Bean
        fun loadInitialData(taskRepository: TaskRepository): CommandLineRunner {
            return CommandLineRunner {
                if (taskRepository.count() == 0L) {
                    val listOf = arrayListOf(
                        Task(name = "Learn new technologies", createdByUser = "default@admin.com"),
                        Task(name = "Travel around the world", createdByUser = "default@admin.com"),
                        Task(name = "Fix the Laptop", createdByUser = "default@admin.com")
                    )
                    taskRepository.saveAll(listOf)
                }
            }
        }

        @Bean
        fun jwtDecoder(properties: AuthenticationProperties): JwtDecoder? {
            val jwt = properties.jwt
            return if (jwt != null && jwt.keyValue != null) {
                val secretKeySpec = SecretKeySpec(jwt.keyValue.toByteArray(StandardCharsets.UTF_8), "HS256")
                NimbusJwtDecoder.withSecretKey(secretKeySpec).build()
            } else {
                val publicKey = applicationContext.getBean(RSAPublicKey::class.java)
                NimbusJwtDecoder.withPublicKey(publicKey).build()
            }
        }

        override fun setApplicationContext(applicationContext: ApplicationContext) {
            this.applicationContext = applicationContext
        }
    }

    @Test
    @DisplayName("When Calling POST - /api/tasks should create task response 201 - Created")
    fun shouldCreateAndDeleteTaskWhenCallingApi() {
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("dummy_user", null, listOf(SimpleGrantedAuthority("ROLE_TASK_CREATE"), SimpleGrantedAuthority("ROLE_TASK_DELETE")))

        val task = TaskDto(name = "New Task")

        val headers = HttpHeaders()
        headers.add("authorization", jwtTokenUtil.createToken(usernamePasswordAuthenticationToken))
        var responseEntity = restTemplate.exchange("/api/tasks", HttpMethod.POST, HttpEntity(task, headers), String::class.java)

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(read<String>(responseEntity.body, "$.id")).isNotEmpty
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
        headers.add(HttpHeaders.AUTHORIZATION, jwtTokenUtil.createToken(usernamePasswordAuthenticationToken))
        var responseEntity = restTemplate.exchange("/api/tasks", HttpMethod.GET, HttpEntity(null, headers), String::class.java)

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(read<List<String>>(responseEntity.body, "$.content")).isEmpty()
        assertThat(read<Integer>(responseEntity.body, "$.totalElements")).isEqualTo(0)

        headers = HttpHeaders()
        headers.add("authorization", jwtTokenUtil.createToken(usernamePasswordAuthenticationToken))
        responseEntity = restTemplate.exchange("/api/tasks", HttpMethod.GET, HttpEntity(null, headers), String::class.java)

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(read<List<String>>(responseEntity.body, "$.content")).isEmpty()
    }

    @Test
    @DisplayName("When Calling GET - /api/tasks should return list of tasks and response 200 - OK")
    fun shouldReturnListOfTasksWhenCallingApi() {
        val headers = HttpHeaders()
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("user", null, listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
        headers.add(HttpHeaders.AUTHORIZATION, jwtTokenUtil.createToken(usernamePasswordAuthenticationToken))
        var responseEntity = restTemplate.exchange("/api/tasks", HttpMethod.GET, HttpEntity(null, headers), String::class.java)

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(read<List<String>>(responseEntity.body, "$.content[*].id")).hasSize(3)
        assertThat(read<List<String>>(responseEntity.body, "$..createdByUser").distinct()).containsExactly("default@admin.com")

        responseEntity = restTemplate.exchange("/api/tasks?name=ar", HttpMethod.GET, HttpEntity(null, headers), String::class.java)

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(read<List<String>>(responseEntity.body, "$.content[*].id")).hasSize(2)

        responseEntity = restTemplate.exchange("/api/tasks?name=Learn", HttpMethod.GET, HttpEntity(null, headers), String::class.java)

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(read<List<String>>(responseEntity.body, "$.content[*].id")).hasSize(1)

        responseEntity = restTemplate.exchange("/api/tasks?THE", HttpMethod.GET, HttpEntity(null, headers), String::class.java)

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(read<List<String>>(responseEntity.body, "$.content[*].id")).hasSize(2)

        responseEntity = restTemplate.exchange("/api/tasks?name=Something else", HttpMethod.GET, HttpEntity(null, headers), String::class.java)

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(read<List<String>>(responseEntity.body, "$.content")).isEmpty()
    }

    @Test
    @DisplayName("When Calling GET - /api/tasks with different role should response 403 - Forbidden")
    fun shouldResponseForbiddenWhenRoleIsNotAppropriatedToListOfTask() {
        val headers = HttpHeaders()
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("user", null, listOf(SimpleGrantedAuthority("ROLE_PERSON_DELETE")))
        headers.add(HttpHeaders.AUTHORIZATION, jwtTokenUtil.createToken(usernamePasswordAuthenticationToken))
        val responseEntity = restTemplate.exchange("/api/tasks", HttpMethod.GET, HttpEntity(null, headers), String::class.java)

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/tasks/{id} without valid authorization should response 403 - Forbidden")
    fun shouldResponseForbiddenWhenCallGetApiWithoutRightPermission() {
        val id: String? = taskRepository.findAll().iterator().next().id
        val headers = HttpHeaders()
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("user", null, listOf(SimpleGrantedAuthority("ROLE_TASK_READ")))
        headers.add(HttpHeaders.AUTHORIZATION, jwtTokenUtil.createToken(usernamePasswordAuthenticationToken))
        val responseEntity = restTemplate.exchange<String>("/api/tasks/$id", HttpMethod.GET, HttpEntity(null, headers))

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(read<String>(responseEntity.body, "$.message")).contains("User(user) does not have access to this resource")
    }

}
