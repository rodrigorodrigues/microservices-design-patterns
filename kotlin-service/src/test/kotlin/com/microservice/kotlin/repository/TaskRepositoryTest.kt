package com.microservice.kotlin.repository

import com.microservice.kotlin.model.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*

@ExtendWith(SpringExtension::class)
@DataMongoTest
class TaskRepositoryTest(@Autowired private val taskRepository: TaskRepository) {
    @BeforeEach
    fun setup() {
        val listOf = arrayListOf<Task>()
        var task = Task(UUID.randomUUID().toString(), "Fix Computer", "rodrigo", lastModifiedByUser = "rodrigo")
        listOf.add(task)

        task = Task(UUID.randomUUID().toString(), "Fix Laptop", "gustavo", lastModifiedByUser = "rodrigo")
        listOf.add(task)

        task = Task(UUID.randomUUID().toString(), "Fix TV", "rodrigo", lastModifiedByUser = "rodrigo")
        listOf.add(task)

        taskRepository.saveAll(listOf)
    }

    @Test
    fun testFindAllByCreatedByUser() {
        var list = taskRepository.findAllByCreatedByUser("rodrigo")

        assertThat(list.size).isEqualTo(2)

        list = taskRepository.findAllByCreatedByUser("gustavo")

        assertThat(list.size).isEqualTo(1)

        list = taskRepository.findAllByCreatedByUser("juninho")

        assertThat(list.isEmpty()).isTrue()
    }

    @AfterEach
    fun tearDown() = taskRepository.deleteAll()
}
