package com.learning.springboot.mapper;

import com.learning.springboot.dto.PersonDto;
import com.learning.springboot.model.Person;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PersonMapper {
    Person dtoToEntity(PersonDto personDto);

    List<Person> dtoToEntity(List<PersonDto> persons);

    PersonDto entityToDto(Person person);

    List<PersonDto> entityToDto(List<Person> persons);
}
