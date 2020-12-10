package com.microservice.kotlin.repository

import com.microservice.kotlin.model.Task
import org.springframework.data.querydsl.QuerydslPredicateExecutor
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository

@Repository
interface TaskRepository : PagingAndSortingRepository<Task, String>, QuerydslPredicateExecutor<Task> {
    fun findAllByCreatedByUser(user: String): List<Task>
}
