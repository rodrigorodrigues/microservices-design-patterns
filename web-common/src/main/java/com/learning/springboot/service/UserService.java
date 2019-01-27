package com.learning.springboot.service;

import com.learning.springboot.dto.UserDto;
import com.learning.springboot.model.User;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import reactor.core.publisher.Mono;

public interface UserService extends ReactiveUserDetailsService {
    Mono<UserDto> findByEmail(String email);
    Mono<User> findSystemDefaultUser();
}
