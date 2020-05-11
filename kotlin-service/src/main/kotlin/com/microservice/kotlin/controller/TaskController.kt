package com.microservice.kotlin.controller

import com.microservice.kotlin.model.Task
import com.microservice.kotlin.repository.TaskRepository
import io.swagger.annotations.ApiParam
import org.apache.commons.lang.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PostFilter
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import springfox.documentation.annotations.ApiIgnore
import java.net.URI
import java.time.Instant
import javax.validation.Valid

@RestController
@RequestMapping("/api/tasks")
class TaskController(@Autowired val repository: TaskRepository) {

    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAnyRole('ADMIN', 'TASK_READ', 'TASK_SAVE')")
    fun findById(@PathVariable id: String,
                 @ApiIgnore @AuthenticationPrincipal authentication: Authentication): Task = repository.findById(id)
        .map {
            if (authentication.hasAdminAuthority() || it.wasCreatedBy(authentication.name)) {
                it
            } else {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "User(${authentication.name}) does not have access to this resource")
            }
        }
        .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }

    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAnyRole('ADMIN', 'TASK_READ', 'TASK_SAVE', 'TASK_DELETE', 'TASK_CREATE')")
    @PostFilter("hasRole('ADMIN') or filterObject.createdByUser == authentication.name")
    fun findAll(): List<Task> {
        val findAll = repository.findAll()
        return if (findAll.count() == 0) {
            mutableListOf()
        } else {
            findAll.toList()
        }
    }

    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAnyRole('ADMIN', 'TASK_CREATE')")
    fun create(@RequestBody @Valid @ApiParam(required = true) task: Task,
               @ApiIgnore @AuthenticationPrincipal authentication: Authentication): ResponseEntity<Task> {
        if (StringUtils.isNotBlank(task.id)) {
            return update(task, task.id!!)
        }
        task.createdByUser = authentication.name
        task.createdDate = Instant.now()
        repository.save(task)
        return ResponseEntity.created(URI.create("/api/tasks/${task.id}"))
            .body(task)
    }

    @PutMapping(value = ["/{id}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAnyRole('ADMIN', 'TASK_SAVE') and (hasRole('ADMIN') or #task.createdByUser == authentication.name)")
    fun update(@RequestBody @ApiParam(required = true) task: Task,
               @PathVariable @ApiParam(required = true) id: String): ResponseEntity<Task> {
        task.id = id
        return repository.findById(id)
            .map { ResponseEntity.ok(repository.save(task)) }
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TASK_DELETE')")
    fun delete(@PathVariable @ApiParam(required = true) id: String,
               @ApiIgnore @AuthenticationPrincipal authentication: Authentication) = repository.findById(id)
        .map {
            if (authentication.authorities.contains(SimpleGrantedAuthority("ROLE_ADMIN")) || it.createdByUser == authentication.name) {
                repository.deleteById(id)
            } else {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "User(${authentication.name}) does not have access to this resource")
            }
        }

    private fun Authentication.hasAdminAuthority() = SimpleGrantedAuthority("ROLE_ADMIN") in this.authorities

    private fun Task.wasCreatedBy(name: String) = this.createdByUser == name
}
