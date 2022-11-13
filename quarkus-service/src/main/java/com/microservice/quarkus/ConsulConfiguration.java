package com.microservice.quarkus;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

import com.orbitz.consul.Consul;
import io.quarkus.arc.profile.IfBuildProfile;

@Dependent
@IfBuildProfile("consul")
public class ConsulConfiguration {
    @Produces
    Consul consulClient = Consul.builder()
            .withUrl(System.getProperty("CONSUL_CLIENT_URL", "http://service-discovery:8500"))
            .build();
}
