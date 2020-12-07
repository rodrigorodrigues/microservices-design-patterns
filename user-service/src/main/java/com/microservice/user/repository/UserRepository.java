package com.microservice.user.repository;

import com.microservice.user.model.User;
import org.springframework.data.querydsl.ReactiveQuerydslPredicateExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveCrudRepository<User, String>, ReactiveQuerydslPredicateExecutor<User> {
    Mono<User> findByEmail(String email);
}
