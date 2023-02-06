package com.microservice.quarkus;

import com.orbitz.consul.Consul;
import com.orbitz.consul.DummyConsul;
import io.quarkus.runtime.configuration.ProfileManager;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

@Dependent
public class ConsulConfiguration {
    @Produces
    Consul consul() {
        if (ProfileManager.getActiveProfile().contains("consul")) {
            return Consul.builder()
                    .withUrl(System.getProperty("CONSUL_CLIENT_URL", "http://localhost:8500"))
                    .build();
        } else {
            return new DummyConsul();
        }
    }
}
