package com.learning.springboot.repository;

import com.learning.springboot.model.Address;
import com.learning.springboot.model.Authority;
import com.learning.springboot.model.Child;
import com.learning.springboot.model.Person;
import com.learning.springboot.util.ReactiveMongoMetadataUtil;
import com.mongodb.reactivestreams.client.MongoCollection;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataMongoTest(properties = {"configuration.initialLoad=false"})
@Import(ReactiveMongoMetadataUtil.class)
public class PersonRepositoryTest {
    @Autowired
    PersonRepository personRepository;

    @Autowired
    ReactiveMongoMetadataUtil reactiveMongoMetadataUtil;

    @BeforeEach
    @Transactional
    public void setup() {
        Mono<MongoCollection<Document>> recreateCollection = reactiveMongoMetadataUtil.recreateCollection(Person.class);

        StepVerifier.create(recreateCollection).expectNextCount(1).verifyComplete();

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

    @Test
    @DisplayName("Return list of People")
    public void findAllStream() {
        StepVerifier.create(personRepository.findAll())
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    public void findAllByNameStartingWithShouldReturnPersonsThatNameStartsWithA() throws Exception {
        Queue<Person> people = new ConcurrentLinkedQueue<>();

        Disposable disposable = personRepository.findAllByNameIgnoreCaseStartingWith("a")
                .doOnNext(people::add)
                .subscribe();

        TimeUnit.MILLISECONDS.sleep(100);

        disposable.dispose();

        assertThat(people.size()).isEqualTo(2);
        assertThat(Stream.of(people.toArray(new Person[] {})).map(Person::getName))
                .containsExactlyInAnyOrder("Anna Cio", "Anonymous");
    }

    @Test
    public void findByChildrenExistsShouldReturnPersonsThatHasChild() throws InterruptedException {
        Queue<Person> people = new ConcurrentLinkedQueue<>();

        Disposable disposable = personRepository.findByChildrenExists(true)
                .doOnNext(people::add)
                .subscribe();

        TimeUnit.MILLISECONDS.sleep(100);

        disposable.dispose();

        assertThat(people.size()).isEqualTo(1);
        assertThat(people.peek().getName()).isEqualTo("Rodrigo");
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