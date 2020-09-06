package com.microservice.user.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.authentication.common.model.Authority;
import com.microservice.authentication.common.service.SharedAuthenticationService;
import com.microservice.user.model.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;

@Slf4j
@ExtendWith(SpringExtension.class)
@DataMongoTest(properties = {"configuration.initialLoad=false", "logging.level.com.microservice.person.util=debug"})
@Import({ObjectMapper.class, UserRepositoryTest.MockServiceConfiguration.class})
class UserRepositoryTest {

    @TestConfiguration
    static class MockServiceConfiguration {
        @Bean
        SharedAuthenticationService sharedAuthenticationService() {
            return mock(SharedAuthenticationService.class);
        }
    }

    @Autowired
    UserRepository userRepository;

    @BeforeEach
    public void setup() {
        List<Authority> authorities = Arrays.asList(new Authority("ROLE_ADMIN"));

        userRepository.saveAll(Arrays.asList(
                User.builder().fullName("Test 1").authorities(authorities).email("test@gmail.com").password("Test@1").build(),
                User.builder().fullName("Test 2").authorities(authorities).email("test_2@gmail.com").password("Test@1").build()
        )).blockLast();
    }

    @AfterEach
    public void tearDown() {
        userRepository.deleteAll().subscribe(a -> log.debug("Delete all users"));
    }

    @Test
    void shouldFindByEmail() {
        StepVerifier.create(userRepository.findByEmail("test@gmail.com"))
                .expectNextMatches(u -> u.getId() != null && "test@gmail.com".equals(u.getEmail()) && u.getEnabled())
                .expectComplete()
                .verify();
    }

    @Test
    void shouldReturnNullWhenNotFindEmail() {
        StepVerifier.create(userRepository.findByEmail("test221@gmail.com"))
                .expectNextCount(0)
                .verifyComplete();
    }
}