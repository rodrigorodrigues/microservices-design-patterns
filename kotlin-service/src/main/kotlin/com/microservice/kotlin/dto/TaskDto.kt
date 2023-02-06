package com.microservice.kotlin.dto

import java.time.Instant

data class TaskDto(
    var id: String? = null,
    var name: String,
    var createdByUser: String? = null,
    var createdDate: Instant? = null,
    var lastModifiedByUser: String? = null,
    var lastModifiedDate: Instant? = null,
    var postId: String? = null,
    var personId: String? = null,
    var requestId: String? = null
)
