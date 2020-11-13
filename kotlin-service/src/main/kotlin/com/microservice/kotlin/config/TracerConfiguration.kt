package com.microservice.kotlin.config

import io.opentracing.Tracer
import io.opentracing.noop.NoopTracerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@ConditionalOnProperty(value = ["opentracing.jaeger.enabled"], havingValue = "false")
@Configuration
class TracerConfiguration {
    @Bean
    fun jaegerTracer(): Tracer? {
        return NoopTracerFactory.create()
    }
}
