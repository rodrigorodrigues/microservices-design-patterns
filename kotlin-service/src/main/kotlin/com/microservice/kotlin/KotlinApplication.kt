package com.microservice.kotlin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient

@SpringBootApplication
@EnableDiscoveryClient
class KotlinApp

    fun main(args: Array<String>) {
        runApplication<KotlinApp>(*args)
    }
