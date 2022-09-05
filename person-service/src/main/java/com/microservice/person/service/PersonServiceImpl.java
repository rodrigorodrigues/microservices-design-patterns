package com.microservice.person.service;

import java.lang.reflect.Type;
import java.util.List;

import com.microservice.person.config.ConfigProperties;
import com.microservice.person.dto.PersonDto;
import com.microservice.person.mapper.PersonMapper;
import com.microservice.person.model.Person;
import com.microservice.person.repository.PersonRepository;
import com.querydsl.core.types.Predicate;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
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

    private final ParameterizedTypeReference<List<PersonDto.Post>> parameterizedTypeReference = new ParameterizedTypeReference<>() { };

    private void processPost(Page<PersonDto> page, String authorization) {
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add(HttpHeaders.AUTHORIZATION, authorization);
            HttpEntity httpEntity = new HttpEntity(httpHeaders);
            ResponseEntity<List<PersonDto.Post>> entity = restTemplate.exchange(configProperties.getPostApi(), HttpMethod.GET, httpEntity, parameterizedTypeReference);
            page.getContent().forEach(p -> p.setPosts(entity.getBody()));
        } catch (Exception e) {
            log.warn("Could not process post api", e);
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
        return people;
    }

    @Override
    public Page<PersonDto> findAllByCreatedByUser(String createdByUser, Pageable pageable, Predicate predicate, String authorization) {
        Page<PersonDto> people = personMapper.entityToDto(personRepository.findAllByCreatedByUser(createdByUser, pageable, predicate), personRepository.count(predicate));
        processPost(people, authorization);
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
}
