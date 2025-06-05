package com.microservice.person.service;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.microservice.person.config.ConfigProperties;
import com.microservice.person.dto.PersonDto;
import com.microservice.person.mapper.PersonMapper;
import com.microservice.person.model.Person;
import com.microservice.person.repository.PersonRepository;
import com.querydsl.core.types.Predicate;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@AllArgsConstructor
public class PersonServiceImpl implements PersonService {
    private final PersonRepository personRepository;

    private final PersonMapper personMapper;

    private final RestTemplate restTemplate;

    private final ConfigProperties configProperties;

    private final Environment environment;

    private final ParameterizedTypeReference<CustomPageImpl<PersonDto.Post>> parameterizedTypeReference = new ParameterizedTypeReference<>() { };

    private void processPost(Page<PersonDto> page, String authorization) {
        if (environment.acceptsProfiles(Profiles.of("callPostApi")) && !page.isEmpty()) {
            try {
                for (PersonDto person : page.getContent()) {
                    HttpHeaders httpHeaders = new HttpHeaders();
                    httpHeaders.add(HttpHeaders.AUTHORIZATION, authorization);
                    HttpEntity httpEntity = new HttpEntity(httpHeaders);
                    ResponseEntity<CustomPageImpl<PersonDto.Post>> entity = restTemplate.exchange(configProperties.getPostApi() + "?personId="+person.getId(), HttpMethod.GET, httpEntity, parameterizedTypeReference);
                    person.setPosts(entity.getBody().getContent());
                }
            }
            catch (Exception e) {
                log.warn("Could not process post api", e);
            }
        }
    }

    private void processUser(Page<PersonDto> page) {
        if (environment.acceptsProfiles(Profiles.of("callUserApi")) && !page.isEmpty()) {
            try {
                for (PersonDto person : page.getContent()) {
                    HttpHeaders httpHeaders = new HttpHeaders();
                    HttpEntity httpEntity = new HttpEntity(httpHeaders);
                    ResponseEntity<PersonDto.User> entity = restTemplate.exchange(configProperties.getUserApi() + "?personId="+person.getId(), HttpMethod.GET, httpEntity, PersonDto.User.class);
                    person.setUser(entity.getBody());
                }
            }
            catch (Exception e) {
                log.warn("Could not process user api", e);
            }
        }
    }

    @Override
    public PersonDto save(PersonDto personDto) {
        Person person = personMapper.dtoToEntity(personDto);
        return personMapper.entityToDto(personRepository.save(person));
    }

    @Override
    public PersonDto findById(String id) {
        return personMapper.entityToDto(personRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    @Override
    public Page<PersonDto> findAll(Pageable pageable, Predicate predicate, String authorization) {
        Page<PersonDto> people = personMapper.entityToDto(personRepository.findAll(predicate, pageable), personRepository.count(predicate));
        processPost(people, authorization);
        processUser(people);
        return people;
    }

    @Override
    public Page<PersonDto> findAllByCreatedByUser(String createdByUser, Pageable pageable, Predicate predicate, String authorization) {
        Page<PersonDto> people = personMapper.entityToDto(personRepository.findAllByCreatedByUser(createdByUser, pageable, predicate), personRepository.count(predicate));
        processPost(people, authorization);
        processUser(people);
        return people;
    }

    @Override
    public Page<PersonDto> findAllByNameStartingWith(String name, Pageable pageable, Predicate predicate) {
        return personMapper.entityToDto(personRepository.findAllByFullNameIgnoreCaseStartingWith(name, pageable, predicate), personRepository.count(predicate));
    }

    @Override
    public Page<PersonDto> findByChildrenExists(Pageable pageable, Predicate predicate) {
        return personMapper.entityToDto(personRepository.findByChildrenExists(true, pageable, predicate), personRepository.count(predicate));
    }

    @Override
    public void deleteById(String id) {
        personRepository.deleteById(id);
    }

    private static class CustomPageImpl<T> extends PageImpl<T> {
        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public CustomPageImpl(@JsonProperty("content") List<T> content, @JsonProperty("number") int number,
            @JsonProperty("size") int size, @JsonProperty("totalElements") Long totalElements,
            @JsonProperty("pageable") JsonNode pageable, @JsonProperty("last") boolean last,
            @JsonProperty("totalPages") int totalPages, @JsonProperty("sort") JsonNode sort,
            @JsonProperty("numberOfElements") int numberOfElements) {
            super(content, PageRequest.of(number, 1), 10);
        }

        public CustomPageImpl(List<T> content, Pageable pageable, long total) {
            super(content, pageable, total);
        }

        public CustomPageImpl(List<T> content) {
            super(content);
        }

        public CustomPageImpl() {
            super(new ArrayList<>());
        }
    }
}
