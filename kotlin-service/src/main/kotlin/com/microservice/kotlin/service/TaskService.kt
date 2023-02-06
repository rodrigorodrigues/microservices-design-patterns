package com.microservice.kotlin.service

import com.microservice.kotlin.dto.TaskDto
import com.microservice.kotlin.mapper.TaskMapper
import com.microservice.kotlin.repository.TaskRepository
import com.querydsl.core.types.Predicate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class TaskService(@Autowired val taskMapper: TaskMapper, @Autowired val taskRepository: TaskRepository) {
    fun save(taskDto: TaskDto) = taskMapper.entityToDto(taskRepository.save(taskMapper.dtoToEntity(taskDto)))

    fun findById(id: String) = taskMapper.entityToDto(taskRepository.findById(id)
        .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) })

    fun findAll(predicate: Predicate, pageable: Pageable): Page<TaskDto> =
        taskMapper.entityToDto(taskRepository.findAll(predicate, pageable), taskRepository.count(predicate))

    fun findAll(predicate: Predicate, pageable: Pageable, authenticationName: String, postId: String?): Page<TaskDto> {
        return taskMapper.entityToDto(if (postId != null) {
            taskRepository.findAllByCreatedByUserAndPostId(authenticationName, postId, pageable)
        } else {
            taskRepository.findAllByCreatedByUser(authenticationName, pageable)
        }, taskRepository.count(predicate))
    }

    fun deleteById(id: String) = taskRepository.deleteById(id)
}
