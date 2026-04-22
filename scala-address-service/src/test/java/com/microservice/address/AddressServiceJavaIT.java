package com.microservice.address;

import com.microservice.address.ScalaAddressServiceConfig;
import com.microservice.address.models.Address;
import com.microservice.address.repository.AddressRepository;
import com.microservice.address.routes.AddressRoutes;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.keys.HmacKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import scala.Option;
import scala.concurrent.ExecutionContext;

import java.util.Collections;
import java.util.List;

@SpringBootTest(classes = ScalaAddressServiceConfig.class, properties = {
    "spring.cloud.consul.config.enabled=false",
    "spring.cloud.consul.discovery.enabled=false",
    "spring.cloud.consul.enabled=false",
    "spring.main.allow-bean-definition-overriding=true",
    "spring.data.mongodb.repositories.type=auto"
})
@Import(TestcontainersConfigurationJava.class)
public class AddressServiceJavaIT {

    @Autowired
    private AddressRepository addressRepository;

    @Test
    public void verifyContextStartsAndDatabaseIsAccessible() {
        // Basic sanity check that context and mongo are up
        addressRepository.findAll().collectList().block();
    }
}
