package com.microservice.quarkus;

import io.quarkus.vertx.http.runtime.filters.Filters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class RequestFilters {

    /**
     * Fixes https://github.com/quarkusio/quarkus/issues/6096
     */
    public void filters(@Observes final Filters filters) {
        filters.register(rc -> {
            if ("/actuator".startsWith(rc.request().path())) {
                rc.request().headers().remove("Authorization");
            }
            rc.next();
        }, 9000);
    }

}