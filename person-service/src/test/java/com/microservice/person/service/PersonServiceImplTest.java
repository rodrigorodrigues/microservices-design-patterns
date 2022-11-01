package com.microservice.person.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import com.microservice.person.config.ConfigProperties;
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

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PersonServiceImplTest {

    PersonServiceImpl personService;

    @Mock
    PersonRepository personRepository;

    @Mock
    RestTemplate restTemplate;

    @Mock
    ConfigProperties configProperties;

    @Mock
    Environment environment;

    PersonMapper personMapper = new PersonMapperImpl();

    @BeforeEach
    public void setup() {
        personService = new PersonServiceImpl(personRepository, personMapper, restTemplate, configProperties, environment);
    }

    @Test
    public void whenCallSaveShouldSavePerson() {
        Person person = new Person();
        when(personRepository.save(any())).thenReturn(person);

        PersonDto personDto = new PersonDto();
        assertThat(personService.save(personDto)).isNotNull();
    }

    @Test
    public void whenCallFindByIdShouldFindPerson() {
        when(personRepository.findById(anyString())).thenReturn(Optional.of(new Person()));

        assertThat(personService.findById("test")).isNotNull();
    }

    @Test
    public void whenCallFindAllShouldReturnListOfPersons() {
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);
        when(personRepository.findAll(any(Predicate.class), any(Pageable.class))).thenReturn(new PageImpl<>(Arrays.asList(new Person(), new Person(), new Person())));
        when(configProperties.getPostApi()).thenReturn("mock_url");
        ResponseEntity<Object> responseEntity = ResponseEntity.ok(Collections.singletonList(new PersonDto.Post("1", "Test", null, null, null, null)));
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(ParameterizedTypeReference.class))).thenReturn(responseEntity);

        Page<PersonDto> people = personService.findAll(PageRequest.of(0, 10), QPerson.person.id.isNotNull(), "");

        assertThat(people.getTotalElements()).isEqualTo(3);
    }

    @Test
    public void whenCallFindAllByNameStartingWithShouldReturnListOfPersons() {
        when(personRepository.findAllByFullNameIgnoreCaseStartingWith(anyString(), any(Pageable.class), any(Predicate.class))).thenReturn(new PageImpl(Arrays.asList(new Person(), new Person())));

        Page<PersonDto> people = personService.findAllByNameStartingWith("test", PageRequest.of(0, 10), QPerson.person.id.isNotNull());

        assertThat(people.getTotalElements()).isEqualTo(2);
    }

    @Test
    public void whenCallFindByChildrenExistsShouldReturnListOfPersons() {
        when(personRepository.findByChildrenExists(anyBoolean(), any(Pageable.class), any(Predicate.class))).thenReturn(new PageImpl(Arrays.asList(new Person(), new Person())));

        Page<PersonDto> people = personService.findByChildrenExists(Pageable.unpaged(), QPerson.person.id.isNotNull());

        assertThat(people.getTotalElements()).isEqualTo(2);
    }

    @Test
    public void whenCallDeleteByIdShouldDeletePerson() {
        personService.deleteById("123");

        verify(personRepository).deleteById("123");
    }

}
