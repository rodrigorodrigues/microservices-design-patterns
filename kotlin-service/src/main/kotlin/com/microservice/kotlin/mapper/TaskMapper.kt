package com.microservice.kotlin.mapper

import com.microservice.kotlin.dto.TaskDto
import com.microservice.kotlin.model.Task
import org.mapstruct.Mapper
import org.springframework.data.domain.Page
import org.springframework.data.support.PageableExecutionUtils

@Mapper(componentModel = "spring")
interface TaskMapper {
    fun entityToDto(tasks: Page<Task>, count: Long): Page<TaskDto> {
        return entityToDto(tasks.content).let {
            PageableExecutionUtils.getPage(
                it, tasks.pageable
            ) { count }
        }
    }

    fun entityToDto(people: List<Task>): List<TaskDto>

    fun entityToDto(task: Task): TaskDto

    fun dtoToEntity(taskDto: TaskDto): Task
}
