package com.microservice.person.service;

import com.microservice.person.dto.PersonDto;
import com.microservice.person.mapper.PersonMapper;
import com.microservice.person.model.Person;
import com.microservice.person.repository.PersonRepository;
import com.querydsl.core.types.Predicate;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
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
    public Flux<PersonDto> findAll(Pageable pageable, Predicate predicate) {
        return personMapper.entityToDto(personRepository.findAll(predicate), pageable);
    }

    @Override
    public Flux<PersonDto> findAllByCreatedByUser(String createdByUser, Pageable pageable) {
        return personMapper.entityToDto(personRepository.findAllByCreatedByUser(createdByUser), pageable);
    }

    @Override
    public Flux<PersonDto> findAllByNameStartingWith(String name, Pageable pageable) {
        return personMapper.entityToDto(personRepository.findAllByFullNameIgnoreCaseStartingWith(name), pageable);
    }

    @Override
    public Flux<PersonDto> findByChildrenExists(Pageable pageable) {
        return personMapper.entityToDto(personRepository.findByChildrenExists(true));
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return personRepository.deleteById(id);
    }
}
