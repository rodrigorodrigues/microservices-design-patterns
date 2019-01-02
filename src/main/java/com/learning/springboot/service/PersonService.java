package com.learning.springboot.service;

import com.learning.springboot.dto.PersonDto;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service for Person.
 */
public interface PersonService extends ReactiveUserDetailsService {
    /**
     * Save a person.
     * @param personDto
     * @return personDto
     */
    Mono<PersonDto> save(PersonDto personDto);

    /**
     * Return a Person by id.
     * @param id id
     * @return personDto
     */
    Mono<PersonDto> findById(String id);

    /**
     * Return list of persons.
     * @return list of persons
     */
    Flux<PersonDto> findAll();

    /**
     * Return list of persons by name starting with
     * @param name starts with
     * @return list of persons
     */
    Flux<PersonDto> findAllByNameStartingWith(String name);

    /**
     * Return list of persons that have children.
     * @return list of persons
     */
    Flux<PersonDto> findByChildrenExists();

    /**
     * Delete a person by id.
     * @param id id
     */
    Mono<Void> deleteById(String id);
}
