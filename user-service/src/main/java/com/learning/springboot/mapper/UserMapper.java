package com.learning.springboot.mapper;

import com.learning.springboot.dto.UserDto;
import com.learning.springboot.model.User;
import org.mapstruct.Mapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Mapper(componentModel = "spring")
public interface UserMapper {
    default Mono<UserDto> entityToDto(Mono<User> users) {
        return users.map(p -> {
            p.setPassword(null);
            return map(p);
        });
    }

    default Flux<UserDto> entityToDto(Flux<User> users) {
        return users.map(p -> {
            p.setPassword(null);
            return map(p);
        });
    }

    User dtoToEntity(UserDto userDto);

    UserDto map(User user);
}
