package com.learning.springboot.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.springboot.model.Address;
import com.learning.springboot.model.Child;
import com.learning.springboot.model.Person;
import com.learning.springboot.util.ReactiveMongoMetadataUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.Disposable;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataMongoTest(properties = {"configuration.initialLoad=false", "logging.level.com.learning.springboot.util=debug"})
@Import({ReactiveMongoMetadataUtil.class, ObjectMapper.class})
public class PersonRepositoryTest {
    @Autowired
    PersonRepository personRepository;

    @Autowired
    ReactiveMongoMetadataUtil reactiveMongoMetadataUtil;

    @Autowired
    ReactiveMongoTemplate mongoTemplate;

    @BeforeEach
    @Transactional
    public void setup() {
        reactiveMongoMetadataUtil.recreateCollection(Person.class);

        personRepository.save(Person.builder().fullName("Rodrigo")
                .dateOfBirth(LocalDate.now())
                .address(new Address("50 Main Street", "Bray", "Co. Wicklow", "Ireland", "058 65412"))
                .children(Arrays.asList(new Child("Daniel", 2), new Child("Oliver", 2)))
                .build()).block();

        personRepository.save(Person.builder().fullName("Anna Cio")
                .dateOfBirth(LocalDate.now().minusYears(25))
                .address(new Address("50 Main Street", "Bray", "Co. Wicklow", "Ireland", "058 65412"))
                .build()).block();

        personRepository.save(Person.builder().fullName("Anonymous")
                .dateOfBirth(LocalDate.now().minusYears(30))
                .address(new Address("50 Main Street", "Bray", "Co. Wicklow", "Ireland", "058 65412"))
                .build()).block();
    }

    @Test
    @DisplayName("Test - Return list of People")
    public void findAllStream() {
        StepVerifier.create(personRepository.findAll())
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    @DisplayName("Test - Find All People that name starts with 'a' ignore case")
    public void findAllByNameStartingWithShouldReturnPersonsThatNameStartsWithA() throws Exception {
        Queue<Person> people = new ConcurrentLinkedQueue<>();

        Disposable disposable = personRepository.findAllByFullNameIgnoreCaseStartingWith("a")
                .doOnNext(people::add)
                .subscribe();

        TimeUnit.MILLISECONDS.sleep(100);

        disposable.dispose();

        assertThat(people.size()).isEqualTo(2);
        assertThat(Stream.of(people.toArray(new Person[] {})).map(Person::getFullName))
                .containsExactlyInAnyOrder("Anna Cio", "Anonymous");
    }

    @Test
    @DisplayName("Test - Find All People that have kids.")
    public void findByChildrenExistsShouldReturnPersonsThatHasChild() throws InterruptedException {
        Queue<Person> people = new ConcurrentLinkedQueue<>();

        Disposable disposable = personRepository.findByChildrenExists(true)
                .doOnNext(people::add)
                .subscribe();

        TimeUnit.MILLISECONDS.sleep(100);

        disposable.dispose();

        assertThat(people.size()).isEqualTo(1);
        assertThat(people.peek().getFullName()).isEqualTo("Rodrigo");
    }

    @AfterEach
    public void tearDown() {
        mongoTemplate.dropCollection(Person.class).block();
    }
}
