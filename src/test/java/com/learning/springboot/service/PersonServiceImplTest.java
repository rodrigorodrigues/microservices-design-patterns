package com.learning.springboot.service;

import com.learning.springboot.model.Person;
import com.learning.springboot.repository.PersonRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PersonServiceImplTest {

    PersonServiceImpl personService;

    @Mock
    PersonRepository personRepository;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() {
        personService = new PersonServiceImpl(personRepository);
    }

    @Test
    public void whenCallSaveShouldSavePerson() {
        Person person = new Person();
        when(personRepository.save(any())).thenReturn(person);

        Person save = personService.save(new Person());

        assertThat(save).isEqualTo(person);
    }

    @Test
    public void whenCallFindByIdShouldFindPerson() {
        Optional<Person> person = Optional.of(new Person());
        when(personRepository.findById(anyString())).thenReturn(person);

        Optional<Person> optionalPerson = personService.findById(anyString());

        assertThat(optionalPerson.isPresent()).isTrue();
        assertThat(optionalPerson.get()).isEqualTo(person.get());
    }

    @Test
    public void whenCallFindAllShouldReturnListOfPersons() {
        when(personRepository.findAll()).thenReturn(Arrays.asList(new Person(), new Person(), new Person()));

        List<Person> persons = personService.findAll();

        assertThat(persons.size()).isEqualTo(3);
    }

    @Test
    public void whenCallFindAllByNameStartingWithShouldReturnListOfPersons() {
        when(personRepository.findAllByNameIgnoreCaseStartingWith(anyString())).thenReturn(Arrays.asList(new Person(), new Person()));

        List<Person> persons = personService.findAllByNameStartingWith(anyString());

        assertThat(persons.size()).isEqualTo(2);
    }

    @Test
    public void whenCallFindByChildrenExistsShouldReturnListOfPersons() {
        when(personRepository.findByChildrenExists(anyBoolean())).thenReturn(Arrays.asList(new Person(), new Person()));

        List<Person> persons = personService.findByChildrenExists();

        assertThat(persons.size()).isEqualTo(2);
    }

    @Test
    public void whenCallDeleteByIdShouldDeletePerson() {
        personService.deleteById("123");

        verify(personRepository).deleteById("123");
    }

    @Test
    public void whenCallLoadUserByUsernameShouldReturnPerson() {
        Person person = new Person();
        when(personRepository.findByLogin(anyString())).thenReturn(person);

        UserDetails admin = personService.loadUserByUsername(anyString());

        assertThat(admin).isEqualTo(person);
    }

    @Test
    public void whenCallLoadUserByUsernameWithInvalidUserShouldThrowException() {
        expectedException.expect(UsernameNotFoundException.class);
        expectedException.expectMessage("User(test) not found!");

        personService.loadUserByUsername("test");
    }
}