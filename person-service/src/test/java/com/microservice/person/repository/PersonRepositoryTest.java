package com.microservice.person.repository;

import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.stream.Stream;

import com.microservice.person.TestcontainersConfiguration;
import com.microservice.person.model.Address;
import com.microservice.person.model.Child;
import com.microservice.person.model.Person;
import com.microservice.person.model.QPerson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicateBuilderCustomizer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Import(TestcontainersConfiguration.class)
@DataMongoTest(properties = {"configuration.initialLoad=false",
    "logging.level.com.microservice.person.util=debug",
    "spring.cloud.consul.config.enabled=false",
    "spring.cloud.consul.discovery.enabled=false",
    "spring.main.allow-bean-definition-overriding=true",
    "de.flapdoodle.mongodb.embedded.version=5.0.5"})
public class PersonRepositoryTest {
    @Autowired
    PersonRepository personRepository;

    @TestConfiguration
    static class MockConfiguration {

        @Bean
        public JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }

        @Primary
        @Bean
        RestTemplate restTemplate() {
            return mock(RestTemplate.class);
        }

        @Bean
        QuerydslPredicateBuilderCustomizer querydslPredicateBuilderCustomizer() {
            return mock(QuerydslPredicateBuilderCustomizer.class);
        }
    }

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
