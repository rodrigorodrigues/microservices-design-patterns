package com.microservice.quarkus;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

import com.orbitz.consul.Consul;
import com.orbitz.consul.DummyConsul;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.profile.UnlessBuildProfile;

@Dependent
@UnlessBuildProfile("consul")
public class NoopConsulConfiguration {
    @Produces
    @DefaultBean
    Consul noConsul() {
        return new DummyConsul();
    }
}
