package com.microservice.kotlin.repository

import com.microservice.kotlin.model.QTask
import com.microservice.kotlin.model.Task
import com.querydsl.core.types.dsl.StringPath
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.querydsl.QuerydslPredicateExecutor
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer
import org.springframework.data.querydsl.binding.QuerydslBindings
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository

@Repository
interface TaskRepository : PagingAndSortingRepository<Task, String>, QuerydslPredicateExecutor<Task>, QuerydslBinderCustomizer<QTask> {
    fun findAllByCreatedByUser(createdByUser: String, pageable: Pageable): Page<Task>

    @JvmDefault
    override fun customize(bindings: QuerydslBindings, root: QTask) {
        // Make case-insensitive 'like' filter for all string properties
        bindings.bind(String::class.java)
            .first { obj: StringPath, str: String? -> obj.containsIgnoreCase(str) }

/*
        bindings.bind(root.name).first { path, value ->
            if (value.startsWith("%") && value.endsWith("%")) {
                path.containsIgnoreCase(value.substring(1, value.length - 1));
            } else {
                path.eq(value);
            }
        }
*/
    }
}
