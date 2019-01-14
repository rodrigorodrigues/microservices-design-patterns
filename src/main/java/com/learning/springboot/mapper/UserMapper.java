package com.learning.springboot.mapper;

import com.learning.springboot.dto.UserDto;
import com.learning.springboot.model.User;
import org.mapstruct.Mapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    default Mono<UserDto> entityToDto(Mono<User> person) {
        return person.map(p -> map(p));
    }

    default Flux<UserDto> entityToDto(Flux<User> persons) {
        return persons.map(p -> map(p));
    }

    User dtoToEntity(UserDto userDto);

    List<User> map(List<UserDto> users);

    UserDto map(User user);
}
