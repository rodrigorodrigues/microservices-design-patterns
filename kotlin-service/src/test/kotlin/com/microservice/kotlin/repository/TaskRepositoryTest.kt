package com.microservice.kotlin.repository

import com.microservice.kotlin.TestcontainersConfiguration
import com.microservice.kotlin.model.QTask
import com.microservice.kotlin.model.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.security.oauth2.jwt.JwtDecoder

@Import(TestcontainersConfiguration::class)
@DataMongoTest
class TaskRepositoryTest(@Autowired private val taskRepository: TaskRepository) {
    @TestConfiguration
    internal class MockConfiguration {
        @Bean
        fun jwtDecoder(): JwtDecoder {
            return Mockito.mock(JwtDecoder::class.java)
        }
    }


    @BeforeEach
    fun setup() {
        val listOf = arrayListOf(
            Task(name = "Fix Computer", createdByUser =  "admin", lastModifiedByUser = "admin"),
            Task(name = "Fix Laptop", createdByUser = "anonymous", lastModifiedByUser = "anonymous"),
            Task(name = "Fix TV", createdByUser = "admin", lastModifiedByUser = "test")
        )
        taskRepository.saveAll(listOf)
    }

    @Test
    fun testFindAllByCreatedByUser() {
        var list = taskRepository.findAllByCreatedByUser("admin", Pageable.unpaged())

        assertThat(list.content).hasSize(2)

        list = taskRepository.findAllByCreatedByUser("anonymous", Pageable.unpaged())

        assertThat(list.content).hasSize(1)

        list = taskRepository.findAllByCreatedByUser("something else", Pageable.unpaged())

        assertThat(list.content).hasSize(0)

        list = taskRepository.findAll(QTask.task.name.containsIgnoreCase("Fix"), PageRequest.of(0, 2))

        assertThat(list.content).hasSize(2)
        assertThat(list.content.stream().map { it.name }).containsExactlyInAnyOrder("Fix Computer", "Fix Laptop")
    }

    @AfterEach
    fun tearDown() = taskRepository.deleteAll()
}
