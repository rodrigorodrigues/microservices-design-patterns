package com.microservice.person.service;

import com.microservice.person.dto.PersonDto;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service for Person.
 */
public interface PersonService {
    /**
     * Save a user.
     * @param personDto
     * @return personDto
     */
    PersonDto save(PersonDto personDto);

    /**
     * Return a Person by id.
     * @param id id
     * @return personDto
     */
    PersonDto findById(String id);

    /**
     * Return list of people.
     * @param pageable pagination request
     * @param predicate condition request
     * @return list of people
     */
    Page<PersonDto> findAll(Pageable pageable, Predicate predicate);

    /**
     * Return list of people created by specific user
     * @param createdByUser current user
     * @param pageable pagination request
     * @param predicate condition request
     * @return list of people
     */
    Page<PersonDto> findAllByCreatedByUser(String createdByUser, Pageable pageable, Predicate predicate);

    /**
     * Return list of people by name starting with
     * @param name starts with
     * @param pageable pagination request
     * @param predicate condition request
     * @return list of people
     */
    Page<PersonDto> findAllByNameStartingWith(String name, Pageable pageable, Predicate predicate);

    /**
     * Return list of people that have children.
     * @param pageable pagination request
     * @param predicate condition request
     * @return list of people
     */
    Page<PersonDto> findByChildrenExists(Pageable pageable, Predicate predicate);

    /**
     * Delete a user by id.
     * @param id id
     */
    void deleteById(String id);
}
