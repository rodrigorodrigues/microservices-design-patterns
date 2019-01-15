package com.learning.springboot.service;

import com.learning.springboot.dto.UserDto;
import com.learning.springboot.model.User;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserService extends ReactiveUserDetailsService {
    Mono<UserDto> save(UserDto userDto);
    Mono<UserDto> findById(String id);
    Flux<UserDto> findAll();
    Mono<Void> deleteById(String id);
    Mono<UserDto> findByEmail(String email);
    Mono<User> findSystemDefaultUser();
}
