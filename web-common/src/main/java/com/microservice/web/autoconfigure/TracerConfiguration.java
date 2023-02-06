package com.microservice.web.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@ConditionalOnProperty(value = "opentracing.jaeger.enabled", havingValue = "false")
@Configuration
public class TracerConfiguration {

    /*@Primary
    @Bean
    public io.opentracing.Tracer jaegerTracer() {
        return NoopTracerFactory.create();
    }*/
}
