package com.learning.springboot.service;

import com.learning.springboot.model.Person;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;
import java.util.Optional;

/**
 * Service for Person.
 */
public interface PersonService extends UserDetailsService {
    /**
     * Save a person.
     * @param person
     * @return person
     */
    Person save(Person person);

    /**
     * Return a Person by id.
     * @param id id
     * @return person
     */
    Optional<Person> findById(String id);

    /**
     * Return list of persons.
     * @return list of persons
     */
    List<Person> findAll();

    /**
     * Return list of persons by name starting with
     * @param name starts with
     * @return list of persons
     */
    List<Person> findAllByNameStartingWith(String name);

    /**
     * Return list of persons that have children.
     * @return list of persons
     */
    List<Person> findByChildrenExists();

    /**
     * Delete a person by id.
     * @param id id
     */
    void deleteById(String id);
}
