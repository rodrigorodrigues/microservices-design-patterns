package com.learning.springboot.repository;

import com.learning.springboot.model.Authority;
import com.learning.springboot.model.Child;
import com.learning.springboot.model.Person;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataMongoTest
public class PersonRepositoryTest {
    @Autowired
    PersonRepository personRepository;

    @Before
    public void setup() {
        Person person = new Person("Rodrigo", 23, "rod", "123", Collections.emptyList());
        person.setChildren(Arrays.asList(new Child("Daniel", 2), new Child("Oliver", 2)));
        personRepository.save(person);
        person = new Person("Anna", 25, "admin", "admin", Arrays.asList(new Authority("ADMIN")));
        personRepository.save(person);
        personRepository.save(new Person("Anonymous", 30, "test", "test", Collections.emptyList()));
    }

    @After
    public void tearDown() {
        personRepository.deleteAll();
    }

    @Test
    public void findAllByNameStartingWithShouldReturnPersonsThatNameStartsWithA() {
        List<Person> persons = personRepository.findAllByNameIgnoreCaseStartingWith("a");

        assertThat(persons.size()).isEqualTo(2);
        assertThat(persons.stream().map(Person::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("Anna", "Anonymous");
    }

    @Test
    public void findByChildrenExistsShouldReturnPersonsThatHasChild() {
        List<Person> persons = personRepository.findByChildrenExists(true);

        assertThat(persons.size()).isEqualTo(1);
        assertThat(persons.get(0).getName()).isEqualTo("Rodrigo");
    }

    @Test
    public void findByLoginShouldReturnPerson() {
        Person person = personRepository.findByLogin("admin");

        assertThat(person).isNotNull();
        assertThat(person.getName()).isEqualTo("Anna");
    }

    @Test
    public void findByLoginShouldNotReturnPersonWhenLoginIsNotFound() {
        Person person = personRepository.findByLogin("john");

        assertThat(person).isNull();
    }
}