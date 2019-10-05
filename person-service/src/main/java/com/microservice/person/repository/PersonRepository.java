package com.microservice.person.repository;

import com.microservice.person.model.Person;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * Repository for Person Object using MongoDB.
 * Name convention are binding using Spring Data MongoDB - https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/#repositories.query-methods.query-creation
 */
@Repository
public interface PersonRepository extends ReactiveMongoRepository<Person, String> {
    //TODO Check later how to use @Tailable with React EventSource
//    @Tailable
    Flux<Person> findAllByFullNameIgnoreCaseStartingWith(String name);

//    @Tailable
    Flux<Person> findByChildrenExists(boolean exists);

//    @Tailable
    @Query("{}")
    Flux<Person> findAllStream();
}
