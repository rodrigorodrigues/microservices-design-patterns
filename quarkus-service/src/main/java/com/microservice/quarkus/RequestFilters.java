package com.microservice.quarkus;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import io.quarkus.vertx.http.runtime.filters.Filters;

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