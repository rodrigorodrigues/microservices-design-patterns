package com.microservice.kotlin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient

@SpringBootApplication
@EnableDiscoveryClient
class KotlinApplication

    fun main(args: Array<String>) {
        runApplication<KotlinApplication>(*args)
    }
