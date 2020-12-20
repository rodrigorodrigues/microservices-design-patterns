package com.microservice.person.service;

import com.microservice.person.dto.PersonDto;
import com.microservice.person.mapper.PersonMapper;
import com.microservice.person.model.Person;
import com.microservice.person.repository.PersonRepository;
import com.querydsl.core.types.Predicate;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class PersonServiceImpl implements PersonService {
    private final PersonRepository personRepository;

    private final PersonMapper personMapper;

    public PersonDto save(PersonDto personDto) {
        Person person = personMapper.dtoToEntity(personDto);
        return personMapper.entityToDto(personRepository.save(person));
    }

    @Override
    public PersonDto findById(String id) {
        return personMapper.entityToDto(personRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    @Override
    public Page<PersonDto> findAll(Pageable pageable, Predicate predicate) {
        return personMapper.entityToDto(personRepository.findAll(predicate, pageable), personRepository.count(predicate));
    }

    @Override
    public Page<PersonDto> findAllByCreatedByUser(String createdByUser, Pageable pageable) {
        return personMapper.entityToDto(personRepository.findAllByCreatedByUser(createdByUser, pageable), personRepository.count());
    }

    @Override
    public Page<PersonDto> findAllByNameStartingWith(String name, Pageable pageable) {
        return personMapper.entityToDto(personRepository.findAllByFullNameIgnoreCaseStartingWith(name, pageable), personRepository.count());
    }

    @Override
    public Page<PersonDto> findByChildrenExists(Pageable pageable) {
        return personMapper.entityToDto(personRepository.findByChildrenExists(true, pageable), personRepository.count());
    }

    @Override
    public void deleteById(String id) {
        personRepository.deleteById(id);
    }
}
