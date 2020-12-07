package com.microservice.person.mapper;

import com.microservice.person.dto.PersonDto;
import com.microservice.person.model.Person;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

@Mapper(componentModel = "spring")
public interface PersonMapper {
    default Mono<PersonDto> entityToDto(Mono<Person> person) {
        return person.map(this::map);
    }

    default Flux<PersonDto> entityToDto(Flux<Person> people, Pageable pageable) {
        Flux<Person> flux = people.buffer(pageable.getPageSize(), (pageable.getPageNumber() + 1))
            .elementAt(pageable.getPageNumber(), new ArrayList<>())
            .flatMapMany(Flux::fromIterable);
        return entityToDto(flux);
    }

    default Flux<PersonDto> entityToDto(Flux<Person> people) {
        return people.map(this::map);
    }

    Person dtoToEntity(PersonDto personDto);

    PersonDto map(Person person);
}
