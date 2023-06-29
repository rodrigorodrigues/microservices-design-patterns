package com.microservice.quarkus;

import com.orbitz.consul.Consul;
import io.quarkus.arc.DefaultBean;
import org.mockito.Mockito;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@DefaultBean
@ApplicationScoped
public class DummyConsulApplicationBean {
    @Produces
    Consul consulClient = Mockito.mock(Consul.class);
}
