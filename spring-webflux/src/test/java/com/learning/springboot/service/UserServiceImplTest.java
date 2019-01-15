package com.learning.springboot.service;

import com.learning.springboot.dto.UserDto;
import com.learning.springboot.mapper.UserMapper;
import com.learning.springboot.mapper.UserMapperImpl;
import com.learning.springboot.model.User;
import com.learning.springboot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    UserServiceImpl userService;

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    UserMapper userMapper = new UserMapperImpl();

    @BeforeEach
    public void setup() {
        userService = new UserServiceImpl(userRepository, userMapper, passwordEncoder);
    }

    @Test
    void whenCallSaveShouldSaveUser() {
        UserDto userDto = UserDto.builder()
                .password("123")
                .confirmPassword("123").build();
        when(userRepository.save(any())).thenReturn(Mono.just(new User()));

        StepVerifier.create(userService.save(userDto))
                .expectNextCount(1)
                .verifyComplete();
    }

   @Test
    public void whenCallSaveShouldThrowExceptionIfConfirmPasswordIsNotPresent() {
        UserDto userDto = new UserDto();
        StepVerifier.create(userService.save(userDto))
                .expectError(ResponseStatusException.class)
                .verify();
    }


}