package com.microservice.user.mapper;

import com.microservice.user.dto.UserDto;
import com.microservice.user.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

@Mapper(componentModel = "spring")
public interface UserMapper {
    default Mono<UserDto> entityToDto(Mono<User> users) {
        return users.map(u -> {
            u.setPassword(null);
            return map(u);
        });
    }

    default Flux<UserDto> entityToDto(Flux<User> users, Pageable pageable) {
        return users.buffer(pageable.getPageSize(), (pageable.getPageNumber() + 1))
                .elementAt(pageable.getPageNumber(), new ArrayList<>())
                .flatMapMany(Flux::fromIterable)
                .map(u -> {
                    u.setPassword(null);
                    return map(u);
                });
    }

    User dtoToEntity(UserDto userDto);

    @Mapping(source = "enabled", target = "activated", defaultValue = "true")
    UserDto map(User user);
}
