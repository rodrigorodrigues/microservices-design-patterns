package com.microservice.person.repository;

import com.microservice.person.model.Address;
import com.microservice.person.model.Child;
import com.microservice.person.model.Person;
import com.microservice.person.model.QPerson;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ExtendWith(SpringExtension.class)
@DataMongoTest(properties = {"configuration.initialLoad=false", "logging.level.com.microservice.person.util=debug"})
@ActiveProfiles("test")
public class PersonRepositoryTest {
    @Autowired
    PersonRepository personRepository;

    @BeforeEach
    public void setup() {
        personRepository.save(Person.builder().fullName("Rodrigo")
                .dateOfBirth(LocalDate.now())
                .address(new Address("50 Main Street", "Bray", "Co. Wicklow", "Ireland", "058 65412"))
                .children(Arrays.asList(new Child("Daniel", LocalDate.of(2017, Month.JANUARY, 1)), new Child("Oliver", LocalDate.of(2017, Month.JANUARY, 1))))
                .build());

        personRepository.save(Person.builder().fullName("Anna Cio")
                .dateOfBirth(LocalDate.now().minusYears(25))
                .address(new Address("50 Main Street", "Bray", "Co. Wicklow", "Ireland", "058 65412"))
                .build());

        personRepository.save(Person.builder().fullName("Anonymous")
                .dateOfBirth(LocalDate.now().minusYears(30))
                .address(new Address("50 Main Street", "Bray", "Co. Wicklow", "Ireland", "058 65412"))
                .build());
    }

    @Test
    @DisplayName("Test - Return list of People")
    public void findAllStream() {
        assertThat(personRepository.findAll()).hasSize(3);

        assertThat(personRepository.findAll(QPerson.person.id.isNotNull())).hasSize(3);
    }

    @Test
    @DisplayName("Test - Return size of list of People")
    public void testCount() {
        assertThat(personRepository.count(QPerson.person.id.isNotNull())).isEqualTo(3L);

        assertThat(personRepository.count(QPerson.person.fullName.equalsIgnoreCase("Anonymous"))).isEqualTo(1L);
    }

    @Test
    @DisplayName("Test - Find All People that name starts with 'a' ignore case")
    public void findAllByNameStartingWithShouldReturnPersonsThatNameStartsWithA() {
        Page<Person> people = personRepository.findAllByFullNameIgnoreCaseStartingWith("a", Pageable.unpaged(), QPerson.person.id.isNotNull());

        assertThat(people.getTotalElements()).isEqualTo(2);
        assertThat(Stream.of(people.getContent().toArray(new Person[] {})).map(Person::getFullName))
                .containsExactlyInAnyOrder("Anna Cio", "Anonymous");
    }

    @Test
    @DisplayName("Test - Find All People that have kids.")
    public void findByChildrenExistsShouldReturnPersonsThatHasChild() {
        Page<Person> people = personRepository.findByChildrenExists(true, Pageable.unpaged(), QPerson.person.id.isNotNull());

        assertThat(people.getTotalElements()).isEqualTo(1);
        assertThat(people.getContent().get(0).getFullName()).isEqualTo("Rodrigo");
    }

    @AfterEach
    public void tearDown() {
        personRepository.deleteAll();
    }
}
