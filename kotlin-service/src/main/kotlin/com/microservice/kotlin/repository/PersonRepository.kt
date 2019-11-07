package com.microservice.kotlin.repository

import com.microservice.kotlin.model.Person
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class PersonRepository {
    val persons: MutableList<Person> = mutableListOf(
            Person(1, "Rodrigo", LocalDate.of(1983, 5, 4)),
            Person(2, "Elias", LocalDate.of(1972, 1, 25))
    )

    fun findById(id: Int): Person? {
        return persons.singleOrNull { it.id == id }
    }

    fun findAll(): List<Person> {
        return persons
    }
}