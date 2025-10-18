package com.microservice.kotlin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ImportRuntimeHints

@ImportRuntimeHints(CustomRuntimeHintsRegistrar::class)
@SpringBootApplication
class KotlinApplication

    fun main(args: Array<String>) {
        runApplication<KotlinApplication>(*args)
    }
