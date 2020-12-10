package com.microservice.person.service;

import com.microservice.person.dto.PersonDto;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Pageable;
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
     * Return list of people.
     * @param pageable pagination request
     * @param predicate condition predicate
     * @return list of people
     */
    Flux<PersonDto> findAll(Pageable pageable, Predicate predicate);

    /**
     * Return list of people created by specific user
     * @param createdByUser current user
     * @param pageable pagination request
     * @return list of people
     */
    Flux<PersonDto> findAllByCreatedByUser(String createdByUser, Pageable pageable);

    /**
     * Return list of people by name starting with
     * @param name starts with
     * @param pageable pagination request
     * @return list of people
     */
    Flux<PersonDto> findAllByNameStartingWith(String name, Pageable pageable);

    /**
     * Return list of people that have children.
     * @param pageable pagination request
     * @return list of people
     */
    Flux<PersonDto> findByChildrenExists(Pageable pageable);

    /**
     * Delete a user by id.
     * @param id id
     */
    Mono<Void> deleteById(String id);
}
