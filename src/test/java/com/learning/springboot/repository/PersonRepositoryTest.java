package com.learning.springboot.repository;

import com.learning.springboot.model.Address;
import com.learning.springboot.model.Authority;
import com.learning.springboot.model.Child;
import com.learning.springboot.model.Person;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.test.StepVerifier;

import java.util.Arrays;

@ExtendWith(SpringExtension.class)
@DataMongoTest(properties = {"initialLoad=false"})
public class PersonRepositoryTest {
    @Autowired
    PersonRepository personRepository;

    @BeforeEach
    public void setup() {
        Person person = new Person("Rodrigo", 23, "rod@gmail.com", "rod", "123", Arrays.asList(new Authority("USER")));
        person.setChildren(Arrays.asList(new Child("Daniel", 2), new Child("Oliver", 2)));
        person.setAddress(new Address("50 Main Street", "Bray", "Co. Wicklow", "Ireland", "058 65412"));
        personRepository.save(person).block();

        person = new Person("Anna Cio", 25, "admin@gmail.com", "admin", "admin", Arrays.asList(new Authority("ADMIN")));
        person.setAddress(new Address("50 Main Street", "Bray", "Co. Wicklow", "Ireland", "058 65412"));
        personRepository.save(person).block();

        person = new Person("Anonymous", 30, "anonymous@gmail.com", "test", "test", Arrays.asList(new Authority("ANONYMOUS")));
        person.setAddress(new Address("50 Main Street", "Bray", "Co. Wicklow", "Ireland", "058 65412"));
        personRepository.save(person).block();
    }

    @AfterEach
    public void tearDown() {
        personRepository.deleteAll().block();
    }

    @Test
    public void findAllByNameStartingWithShouldReturnPersonsThatNameStartsWithA() {
        StepVerifier.create(personRepository.findAllByNameIgnoreCaseStartingWith("a"))
                .expectNextMatches(p -> p.getName().equals("Anna Cio"))
                .expectNextMatches(p -> p.getName().equals("Anonymous"))
                .verifyComplete();
    }

    @Test
    public void findByChildrenExistsShouldReturnPersonsThatHasChild() {
        StepVerifier.create(personRepository.findByChildrenExists(true))
                .expectNextMatches(p -> p.getName().equals("Rodrigo"))
                .verifyComplete();
    }

    @Test
    public void findByLoginShouldReturnPerson() {
        StepVerifier.create(personRepository.findByUsername("admin"))
                .expectNextMatches(p -> p.getName().equals("Anna Cio"))
                .verifyComplete();
    }

    @Test
    public void findByLoginShouldNotReturnPersonWhenLoginIsNotFound() {
        StepVerifier.create(personRepository.findByUsername("john"))
                .expectNextCount(0)
                .verifyComplete();
    }
}