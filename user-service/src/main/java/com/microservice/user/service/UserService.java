package com.microservice.user.service;

import com.microservice.user.dto.UserDto;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserService {
    Mono<UserDto> save(UserDto userDto);
    Mono<UserDto> findById(String id);
    Flux<UserDto> findAll(Pageable pageable, Predicate predicate);
    Mono<Void> deleteById(String id);

}
