package com.microservice.kotlin.controller

import com.microservice.kotlin.dto.TaskDto
import com.microservice.kotlin.model.Task
import com.microservice.kotlin.repository.TaskRepository
import com.microservice.kotlin.service.TaskService
import com.querydsl.core.types.Predicate
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.querydsl.binding.QuerydslPredicate
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.time.Instant

@RestController
@RequestMapping("/api/tasks")
class TaskController(@Autowired val taskService: TaskService) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(description = "Api for return list of tasks", security = [SecurityRequirement(name = "bearer-key")])
    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAnyRole('ADMIN', 'TASK_READ', 'TASK_SAVE') or hasAuthority('SCOPE_openid')")
    fun findById(@PathVariable id: String,
                 @Parameter(hidden = true) authentication: Authentication): ResponseEntity<TaskDto> {
        val taskDto = taskService.findById(id)
        if (authentication.hasAdminAuthority() || taskDto.wasCreatedBy(authentication)) {
            return ResponseEntity.ok(taskDto)
        } else {
            throw AccessDeniedException("User(${authentication.name}) does not have access to this resource")
        }
    }

    @Operation(description = "Api for return a task by id", security = [SecurityRequirement(name = "bearer-key")])
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAnyRole('ADMIN', 'TASK_READ', 'TASK_SAVE', 'TASK_DELETE', 'TASK_CREATE') or hasAuthority('SCOPE_openid')")
    fun findAll(@RequestParam(name = "page", defaultValue = "0", required = false) page: Int,
                @RequestParam(name = "size", defaultValue = "10", required = false) size: Int,
                @RequestParam(name = "sort-dir", defaultValue = "desc", required = false) sortDirection: String,
                @RequestParam(name = "sort-idx", defaultValue = "createdDate", required = false) sortIdx: List<String>,
                @Parameter(hidden = true)@QuerydslPredicate(root = Task::class, bindings = TaskRepository::class) predicate: Predicate,
                @Parameter(hidden = true) authentication: Authentication,
                @RequestParam(required = false, name = "postId") postId: String?): ResponseEntity<Page<TaskDto>> {
        log.debug("Predicate: {}", predicate)
        log.debug("PostId: {}", postId)
        val pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), *sortIdx.toTypedArray()))
        return ResponseEntity.ok(if (authentication.hasAdminAuthority()) {
            taskService.findAll(predicate, pageRequest)
        } else {
            taskService.findAll(predicate, pageRequest, authentication.name, postId)
        })
    }

    @Operation(description = "Api for creating a task", security = [SecurityRequirement(name = "bearer-key")])
    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAnyRole('ADMIN', 'TASK_CREATE') or hasAuthority('SCOPE_openid')")
    fun create(@RequestBody @Valid @Parameter(required = true) task: TaskDto,
               @Parameter(hidden = true) authentication: Authentication): ResponseEntity<TaskDto> {
        if (StringUtils.isNotBlank(task.id)) {
            return update(task, task.id!!)
        }
        log.debug("Creating task: {}", task)
        task.createdByUser = authentication.name
        task.createdDate = Instant.now()
        val taskCreated = taskService.save(task)
        task.id = taskCreated.id
        log.debug("Created task: {}", task)
        return ResponseEntity.created(URI.create("/api/tasks/${task.id}"))
            .body(task)
    }

    @Operation(description = "Api for updating a task", security = [SecurityRequirement(name = "bearer-key")])
    @PutMapping(value = ["/{id}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("(hasAnyRole('ADMIN', 'TASK_SAVE') or hasAuthority('SCOPE_openid')) and (hasRole('ADMIN') or #task.createdByUser == authentication.name)")
    fun update(@RequestBody @Parameter(required = true) task: TaskDto,
               @PathVariable @Parameter(required = true) id: String): ResponseEntity<TaskDto> {
        log.info("Updating task: {}", task)
        task.id = id
        taskService.findById(id)
        return ResponseEntity.ok(taskService.save(task))
    }

    @Operation(description = "Api for deleting a task", security = [SecurityRequirement(name = "bearer-key")])
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TASK_DELETE') or hasAuthority('SCOPE_openid')")
    fun delete(@PathVariable @Parameter(required = true) id: String,
               @Parameter(hidden = true) authentication: Authentication) {
        val taskDto = taskService.findById(id)
        if (authentication.hasAdminAuthority() || taskDto.wasCreatedBy(authentication)) {
            taskService.deleteById(id)
        } else {
            throw AccessDeniedException("User(${authentication.name}) does not have access to delete this resource")
        }
    }

    private fun Authentication.hasAdminAuthority() = SimpleGrantedAuthority("ROLE_ADMIN") in this.authorities

    private fun TaskDto.wasCreatedBy(authentication: Authentication) = this.createdByUser == authentication.name
}
