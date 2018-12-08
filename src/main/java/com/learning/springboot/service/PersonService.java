package com.learning.springboot.service;

import com.learning.springboot.model.Person;

import java.util.List;
import java.util.Optional;

public interface PersonService {
    Person save(Person person);
    Optional<Person> findById(String id);
    List<Person> findAll();
    List<Person> findAllByNameStartingWith(String name);
    List<Person> findByChildrenExists();
}
