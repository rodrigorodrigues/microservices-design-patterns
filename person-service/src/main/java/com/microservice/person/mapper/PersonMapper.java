package com.microservice.person.mapper;

import com.microservice.person.dto.PersonDto;
import com.microservice.person.model.Person;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.support.PageableExecutionUtils;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PersonMapper {
    default Page<PersonDto> entityToDto(Page<Person> people, long count) {
        return PageableExecutionUtils.getPage(entityToDto(people.getContent()), people.getPageable(), () -> count);
    }

    List<PersonDto> entityToDto(List<Person> people);

    PersonDto entityToDto(Person person);

    Person dtoToEntity(PersonDto personDto);
}
