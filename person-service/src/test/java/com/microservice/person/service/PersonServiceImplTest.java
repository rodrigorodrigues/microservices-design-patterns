package com.microservice.person.service;

import com.microservice.person.dto.PersonDto;
import com.microservice.person.mapper.PersonMapper;
import com.microservice.person.mapper.PersonMapperImpl;
import com.microservice.person.model.Person;
import com.microservice.person.model.QPerson;
import com.microservice.person.repository.PersonRepository;
import com.querydsl.core.types.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PersonServiceImplTest {

    PersonServiceImpl personService;

    @Mock
    PersonRepository personRepository;

    PersonMapper personMapper = new PersonMapperImpl();

    @BeforeEach
    public void setup() {
        personService = new PersonServiceImpl(personRepository, personMapper);
    }

    @Test
    public void whenCallSaveShouldSavePerson() {
        Person person = new Person();
        when(personRepository.save(any())).thenReturn(Mono.just(person));

        PersonDto personDto = new PersonDto();
        StepVerifier.create(personService.save(personDto))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    public void whenCallFindByIdShouldFindPerson() {
        Mono<Person> person = Mono.just(new Person());
        when(personRepository.findById(anyString())).thenReturn(person);

        StepVerifier.create(personService.findById(anyString()))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    public void whenCallFindAllShouldReturnListOfPersons() {
        when(personRepository.findAll(any(Predicate.class))).thenReturn(Flux.fromIterable(Arrays.asList(new Person(), new Person(), new Person())));

        Flux<PersonDto> people = personService.findAll(PageRequest.of(0, 10), QPerson.person.id.isNotNull());

        StepVerifier.create(people)
            .expectNextCount(3)
            .verifyComplete();
    }

    @Test
    public void whenCallFindAllByNameStartingWithShouldReturnListOfPersons() {
        when(personRepository.findAllByFullNameIgnoreCaseStartingWith(anyString())).thenReturn(Flux.fromIterable(Arrays.asList(new Person(), new Person())));

        Flux<PersonDto> people = personService.findAllByNameStartingWith("test", PageRequest.of(0, 10));

        StepVerifier.create(people)
            .expectNextCount(2)
            .verifyComplete();
    }

    @Test
    public void whenCallFindByChildrenExistsShouldReturnListOfPersons() {
        when(personRepository.findByChildrenExists(anyBoolean())).thenReturn(Flux.fromIterable(Arrays.asList(new Person(), new Person())));

        Flux<PersonDto> people = personService.findByChildrenExists(any(Pageable.class));

        StepVerifier.create(people)
            .expectNextCount(2)
            .verifyComplete();
    }

    @Test
    public void whenCallDeleteByIdShouldDeletePerson() {
        personService.deleteById("123");

        verify(personRepository).deleteById("123");
    }

}
