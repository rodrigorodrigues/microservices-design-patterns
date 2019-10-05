package com.microservice.authentication.repository;

import com.microservice.authentication.common.model.Authentication;
import org.springframework.data.repository.Repository;
import reactor.core.publisher.Mono;

public interface AuthenticationRepository extends Repository<Authentication, String> {
    Mono<Authentication> findByEmail(String email);

    Mono<Authentication> save(Authentication authentication);
}
