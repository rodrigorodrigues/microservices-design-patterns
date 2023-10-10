package com.microservice.quarkus;

import com.orbitz.consul.Consul;
import com.orbitz.consul.DummyConsul;
import io.quarkus.runtime.configuration.ConfigUtils;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Dependent
public class ConsulConfiguration {
    private static final Logger log = LoggerFactory.getLogger(ConsulConfiguration.class);

    @Produces
    Consul consul() {
        if (ConfigUtils.getProfiles().contains("consul")) {
            String consulClientUrl = System.getenv("CONSUL_CLIENT_URL");
            if (StringUtils.isBlank(consulClientUrl)) {
                consulClientUrl = "http://localhost:8500";
            }
            log.info("consul url: {}", consulClientUrl);
            return Consul.builder()
                    .withUrl(consulClientUrl)
                    .build();
        } else {
            return new DummyConsul();
        }
    }
}
