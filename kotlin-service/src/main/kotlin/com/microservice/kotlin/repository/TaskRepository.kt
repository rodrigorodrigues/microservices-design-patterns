package com.microservice.kotlin.repository

import com.microservice.kotlin.model.Task
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TaskRepository : CrudRepository<Task, String> {
    fun findAllByCreatedByUser(user: String): List<Task>
}
