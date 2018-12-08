package com.learning.springboot.service;

import com.learning.springboot.model.Person;
import com.learning.springboot.repository.PersonRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
class PersonServiceImpl implements PersonService {
    private final PersonRepository personRepository;

    public Person save(Person person) {
        return personRepository.save(person);
    }

    @Override
    public Optional<Person> findById(String id) {
        return personRepository.findById(id);
    }

    @Override
    public List<Person> findAll() {
        return personRepository.findAll();
    }

    @Override
    public List<Person> findAllByNameStartingWith(String name) {
        return personRepository.findAllByNameStartingWith(name);
    }

    @Override
    public List<Person> findByChildrenExists() {
        return personRepository.findByChildrenExists();
    }
}
