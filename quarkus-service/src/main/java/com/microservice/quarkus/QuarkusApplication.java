package com.microservice.quarkus;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class QuarkusApplication {
    public static void main(String[] args) {
        Quarkus.run(args);
    }
}
