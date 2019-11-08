package com.microservice.kotlin.model

import org.springframework.data.annotation.*
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size

@Document(collection = "tasks")
data class Task(
    @Id
    var id: String,

    @NotEmpty
    @Size(min = 5, max = 200)
    val name: String,

    @CreatedBy
    val createdByUser: String? = null,

    @CreatedDate
    val createdDate: Instant = Instant.now(),

    @LastModifiedBy
    val lastModifiedByUser: String? = null,

    @LastModifiedDate
    val lastModifiedDate: Instant = Instant.now()
)
