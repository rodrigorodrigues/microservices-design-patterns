package com.microservice.person.mapper;

import com.microservice.person.dto.PersonDto;
import com.microservice.person.model.Person;
import org.mapstruct.Mapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Mapper(componentModel = "spring")
public interface PersonMapper {
    default Mono<PersonDto> entityToDto(Mono<Person> person) {
        return person.map(this::map);
    }

    default Flux<PersonDto> entityToDto(Flux<Person> persons) {
        return persons.map(this::map);
    }

    Person dtoToEntity(PersonDto personDto);

    PersonDto map(Person person);
}
