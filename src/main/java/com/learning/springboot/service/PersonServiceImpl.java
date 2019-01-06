package com.learning.springboot.service;

import com.learning.springboot.dto.PersonDto;
import com.learning.springboot.mapper.PersonMapper;
import com.learning.springboot.model.Person;
import com.learning.springboot.repository.PersonRepository;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class PersonServiceImpl implements PersonService {
    private final PersonRepository personRepository;

    private final PasswordEncoder passwordEncoder;

    private final PersonMapper personMapper;

    public Mono<PersonDto> save(PersonDto personDto) {
        if (StringUtils.isBlank(personDto.getId())) {
            if (!StringUtils.equals(personDto.getPassword(), personDto.getConfirmPassword()) || StringUtils.isBlank(personDto.getConfirmPassword())) {
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirm password is different than password!"));
            }
            personDto.setPassword(passwordEncoder.encode(personDto.getPassword()));
        }
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
        return personMapper.entityToDto(personRepository.findAllByNameIgnoreCaseStartingWith(name));
    }

    @Override
    public Flux<PersonDto> findByChildrenExists() {
        return personMapper.entityToDto(personRepository.findByChildrenExists(true));
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return personRepository.deleteById(id);
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) throws UsernameNotFoundException {
        return personRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException(String.format("User(%s) not found!", username))))
                .map(p -> p);
    }
}
