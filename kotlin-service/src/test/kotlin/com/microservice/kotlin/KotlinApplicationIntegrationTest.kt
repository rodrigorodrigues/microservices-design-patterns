package com.microservice.kotlin

import com.github.tomakehurst.wiremock.client.WireMock
import com.jayway.jsonpath.JsonPath.read
import com.microservice.kotlin.model.Task
import com.microservice.kotlin.repository.TaskRepository
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import lombok.SneakyThrows
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.annotation.Bean
import org.springframework.context.support.GenericApplicationContext
import org.springframework.http.*
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.nio.file.Files
import java.security.Key
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [KotlinApplication::class], properties = ["configuration.swagger=false"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [KotlinApplicationIntegrationTest.GenerateKeyPairInitializer::class], classes = [KotlinApplicationIntegrationTest.PopulateDbConfiguration::class])
@AutoConfigureMockMvc @AutoConfigureWireMock(port = 0)
class KotlinApplicationIntegrationTest(@Autowired val restTemplate: TestRestTemplate,
                                       @Autowired val keyPair: KeyPair) {

    var runAtOnce = AtomicBoolean(true)

    var jwtTokenUtil = JwtTokenUtil(keyPair)

    @TestConfiguration
    class PopulateDbConfiguration {

        @Bean
        fun loadInitialData(taskRepository: TaskRepository): CommandLineRunner {
            return CommandLineRunner {
                if (taskRepository.count() == 0L) {
                    val listOf = arrayListOf(
                        Task(name = "Learn new technologies", createdByUser = "default@admin.com"),
                        Task(name = "Travel around the world", createdByUser = "default@admin.com"),
                        Task(name = "Fix Laptop", createdByUser = "default@admin.com")
                    )
                    taskRepository.saveAll(listOf)
                }
            }
        }
    }

    class GenerateKeyPairInitializer : ApplicationContextInitializer<GenericApplicationContext> {
        @SneakyThrows
        override fun initialize(applicationContext: GenericApplicationContext) {
            val kpg = KeyPairGenerator.getInstance("RSA")
            kpg.initialize(2048)
            val kp = kpg.generateKeyPair()
            val pub = kp.public as RSAPublicKey
            val pvt: Key = kp.private
            val encoder = Base64.getEncoder()
            val privateKeyFile = Files.createTempFile("privateKeyFile", ".key")
            val publicKeyFile = Files.createTempFile("publicKeyFile", ".cert")
            Files.write(privateKeyFile,
                Arrays.asList("-----BEGIN PRIVATE KEY-----", encoder
                    .encodeToString(pvt.encoded), "-----END PRIVATE KEY-----"))
            Files.write(publicKeyFile,
                Arrays.asList("-----BEGIN PUBLIC KEY-----", encoder
                    .encodeToString(pub.encoded), "-----END PRIVATE KEY-----"))
            applicationContext.registerBean(RSAPublicKey::class.java, Supplier { pub })
            applicationContext.registerBean(KeyPair::class.java, Supplier { kp })
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
    }

    @Test
    @DisplayName("When Calling POST - /api/tasks should create task response 201 - Created")
    fun shouldCreateAndDeleteTaskWhenCallingApi() {
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("dummy_user", null, listOf(SimpleGrantedAuthority("ROLE_TASK_CREATE"), SimpleGrantedAuthority("ROLE_TASK_DELETE")))

        val task = Task(name = "New Task")

        val headers = HttpHeaders()
        headers.add("authorization", jwtTokenUtil.createToken(usernamePasswordAuthenticationToken))
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

        responseEntity = restTemplate.exchange("/api/tasks?name=Something else", HttpMethod.GET, HttpEntity(null, headers), String::class.java)

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(read<List<String>>(responseEntity.body, "$.content")).isEmpty()
    }

    @Test
    @DisplayName("When Calling GET - /api/tasks with different role should response 403 - Forbidden")
    fun shouldResponseForbiddenWhenRoleIsNotAppropriatedToListOfTask() {
        val headers = HttpHeaders()
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken("user", null, listOf(SimpleGrantedAuthority("TASK_DELETE")))
        headers.add(HttpHeaders.AUTHORIZATION, jwtTokenUtil.createToken(usernamePasswordAuthenticationToken))
        val responseEntity = restTemplate.exchange("/api/tasks", HttpMethod.GET, HttpEntity(null, headers), String::class.java)

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }
}
