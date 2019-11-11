package com.microservice.kotlin.model

import org.springframework.data.annotation.*
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size

@Document(collection = "tasks")
data class Task(
    @Id
    var id: String? = null,

    @NotEmpty
    @Size(min = 5, max = 200)
    var name: String,

    @CreatedBy
    var createdByUser: String? = null,

    @CreatedDate
    var createdDate: Instant? = null,

    @LastModifiedBy
    var lastModifiedByUser: String? = null,

    @LastModifiedDate
    var lastModifiedDate: Instant? = null
)
