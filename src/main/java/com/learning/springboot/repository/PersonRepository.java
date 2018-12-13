package com.learning.springboot.repository;

import com.learning.springboot.model.Person;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Person Object using MongoDB.
 * Name convention are binding using Spring Data MongoDB - https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/#repositories.query-methods.query-creation
 */
@Repository
public interface PersonRepository extends MongoRepository<Person, String> {
    List<Person> findAllByNameIgnoreCaseStartingWith(String name);
    List<Person> findByChildrenExists(boolean exists);
    Person findByLogin(String username);
}
