package com.microservice.kotlin.controller

import com.microservice.kotlin.model.Task
import com.microservice.kotlin.repository.TaskRepository
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.net.URI

@RestController
@RequestMapping("/api/tasks")
class TaskController {

    @Autowired
    lateinit var repository: TaskRepository

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TASK_READ', 'TASK_SAVE')")
    fun findById(@PathVariable id: String): Task? = repository.findById(id)
        .orElseThrow{ResponseStatusException(HttpStatus.NOT_FOUND)}

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TASK_READ', 'TASK_SAVE', 'TASK_DELETE', 'TASK_CREATE')")
    fun findAll(): MutableIterable<Task> = repository.findAll()

    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAnyRole('ADMIN', 'TASK_CREATE')")
    fun create(@RequestBody @ApiParam(required = true) task: Task): ResponseEntity<Task> {
        repository.save(task)
        return ResponseEntity.created(URI.create(String.format("/api/tasks/%s", task.id)))
            .body(task)
    }

    @PutMapping(value = ["/{id}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAnyRole('ADMIN', 'TASK_SAVE')")
    fun update(@RequestBody @ApiParam(required = true) task: Task, @PathVariable @ApiParam(required = true) id: String): ResponseEntity<Task> {
        task.id = id;
        return repository.findById(id)
            .map { t -> ResponseEntity.ok(repository.save(t)) }
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TASK_DELETE')")
    fun delete(@PathVariable @ApiParam(required = true) id: String) = repository.deleteById(id)
}
