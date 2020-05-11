package com.microservice.person.service;

import com.microservice.person.dto.PersonDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service for Person.
 */
public interface PersonService {
    /**
     * Save a user.
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
     * Return list of users.
     * @return list of users
     */
    Flux<PersonDto> findAll();

    /**
     * Return list of users by name starting with
     * @param name starts with
     * @return list of users
     */
    Flux<PersonDto> findAllByNameStartingWith(String name);

    /**
     * Return list of users that have children.
     * @return list of users
     */
    Flux<PersonDto> findByChildrenExists();

    /**
     * Delete a user by id.
     * @param id id
     */
    Mono<Void> deleteById(String id);
}
