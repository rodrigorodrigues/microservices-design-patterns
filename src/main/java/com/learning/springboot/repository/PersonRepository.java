package com.learning.springboot.repository;

import com.learning.springboot.model.Person;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Tailable;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for Person Object using MongoDB.
 * Name convention are binding using Spring Data MongoDB - https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/#repositories.query-methods.query-creation
 */
@Repository
public interface PersonRepository extends ReactiveMongoRepository<Person, String> {
    @Tailable
    Flux<Person> findAllByNameIgnoreCaseStartingWith(String name);
    @Tailable
    Flux<Person> findByChildrenExists(boolean exists);

    Mono<Person> findByUsername(String username);

    @Tailable
    @Query("{}")
    Flux<Person> findAllStream();
}
