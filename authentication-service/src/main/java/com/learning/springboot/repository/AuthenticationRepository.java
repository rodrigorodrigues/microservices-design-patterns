package com.learning.springboot.repository;

import com.learning.springboot.model.Authentication;
import org.springframework.data.repository.Repository;
import reactor.core.publisher.Mono;

public interface AuthenticationRepository extends Repository<Authentication, String> {
    Mono<Authentication> findByEmail(String email);

    Mono<Authentication> save(Authentication authentication);
}
