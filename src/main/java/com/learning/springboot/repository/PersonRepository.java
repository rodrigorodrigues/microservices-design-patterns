package com.learning.springboot.repository;

import com.learning.springboot.model.Person;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public
interface PersonRepository extends MongoRepository<Person, String> {
    List<Person> findAllByNameStartingWith(String name);
    List<Person> findByChildrenExists();
}
