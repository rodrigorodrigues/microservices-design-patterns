package com.microservice.person.repository;

import com.microservice.person.model.Person;
import org.springframework.data.querydsl.ReactiveQuerydslPredicateExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * Repository for Person Object using MongoDB.
 * Name convention are binding using Spring Data MongoDB - https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/#repositories.query-methods.query-creation
 */
@Repository
public interface PersonRepository extends ReactiveCrudRepository<Person, String>, ReactiveQuerydslPredicateExecutor<Person> {
    //TODO Check later how to use @Tailable with React EventSource
//    @Tailable
    Flux<Person> findAllByFullNameIgnoreCaseStartingWith(String name);

    Flux<Person> findByChildrenExists(boolean exists);

    Flux<Person> findAllByCreatedByUser(String createdByUser);
}
