package com.microservice.kotlin.model

import java.time.LocalDate

data class Person(
        val id: Int,
        val fullName: String,
        val dateOfBirth: LocalDate
)