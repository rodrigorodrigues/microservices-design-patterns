package com.learning.springboot.service;

import com.learning.springboot.dto.PersonDto;
import com.learning.springboot.mapper.PersonMapper;
import com.learning.springboot.mapper.PersonMapperImpl;
import com.learning.springboot.model.Person;
import com.learning.springboot.repository.PersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
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
        when(personRepository.findAllStream()).thenReturn(Flux.fromIterable(Arrays.asList(new Person(), new Person(), new Person())));

        Flux<PersonDto> persons = personService.findAll();

        assertThat(persons.count().block()).isEqualTo(3);
    }

    @Test
    public void whenCallFindAllByNameStartingWithShouldReturnListOfPersons() {
        when(personRepository.findAllByFullNameIgnoreCaseStartingWith(anyString())).thenReturn(Flux.fromIterable(Arrays.asList(new Person(), new Person())));

        Flux<PersonDto> persons = personService.findAllByNameStartingWith(anyString());

        assertThat(persons.count().block()).isEqualTo(2);
    }

    @Test
    public void whenCallFindByChildrenExistsShouldReturnListOfPersons() {
        when(personRepository.findByChildrenExists(anyBoolean())).thenReturn(Flux.fromIterable(Arrays.asList(new Person(), new Person())));

        Flux<PersonDto> persons = personService.findByChildrenExists();

        assertThat(persons.count().block()).isEqualTo(2);
    }

    @Test
    public void whenCallDeleteByIdShouldDeletePerson() {
        personService.deleteById("123");

        verify(personRepository).deleteById("123");
    }

}