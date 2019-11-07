package com.microservice.kotlin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KotlinApp

fun main(args: Array<String>) {
    runApplication<KotlinApp>(*args)
}