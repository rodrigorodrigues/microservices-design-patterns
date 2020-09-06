package com.microservice.person.service;

import com.microservice.person.dto.PersonDto;
import com.microservice.person.mapper.PersonMapper;
import com.microservice.person.model.Person;
import com.microservice.person.repository.PersonRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class PersonServiceImpl implements PersonService {
    private final PersonRepository personRepository;

    private final PersonMapper personMapper;

    public Mono<PersonDto> save(PersonDto personDto) {
        Person person = personMapper.dtoToEntity(personDto);
        return personMapper.entityToDto(personRepository.save(person));
    }

    @Override
    public Mono<PersonDto> findById(String id) {
        return personMapper.entityToDto(personRepository.findById(id));
    }

    @Override
    public Flux<PersonDto> findAll() {
        return personMapper.entityToDto(personRepository.findAll());
    }

    @Override
    public Flux<PersonDto> findAllByNameStartingWith(String name) {
        return personMapper.entityToDto(personRepository.findAllByFullNameIgnoreCaseStartingWith(name));
    }

    @Override
    public Flux<PersonDto> findByChildrenExists() {
        return personMapper.entityToDto(personRepository.findByChildrenExists(true));
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return personRepository.deleteById(id);
    }
}
