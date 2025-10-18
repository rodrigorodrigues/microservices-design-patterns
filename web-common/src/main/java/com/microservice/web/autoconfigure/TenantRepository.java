package com.microservice.web.autoconfigure;

import java.net.URL;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class TenantRepository {
    private final Map<String, String> tenants;

    TenantRepository(WebConfigurationProperties properties) {
        this.tenants = properties.getTenants();
    }

    public String findById(URL url) {
        log.info("TenantRepository:findById:url: {}", url);
        return Optional.ofNullable(tenants.get(url.toString()))
                .orElseThrow(() -> new IllegalArgumentException("unknown tenant: " + url));
    }

    public String findById(String id) {
        log.info("TenantRepository:findById:id: {}", id);
        return Optional.ofNullable(tenants.get(id))
                .orElseThrow(() -> new IllegalArgumentException("unknown tenant: " + id));
    }
}
