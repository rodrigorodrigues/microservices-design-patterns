package com.microservice.quarkus;

import com.orbitz.consul.Consul;
import io.quarkus.arc.DefaultBean;
import org.mockito.Mockito;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@DefaultBean
@ApplicationScoped
public class DummyConsulApplicationBean {
    @Produces
    Consul consulClient = Mockito.mock(Consul.class);
}
