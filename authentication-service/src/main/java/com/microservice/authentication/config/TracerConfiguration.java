package com.microservice.authentication.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@ConditionalOnProperty(value = "opentracing.jaeger.enabled", havingValue = "false")
@Configuration
public class TracerConfiguration {

    @Primary
    @Bean
    public io.opentracing.Tracer jaegerTracer() {
        return io.opentracing.noop.NoopTracerFactory.create();
    }
}
