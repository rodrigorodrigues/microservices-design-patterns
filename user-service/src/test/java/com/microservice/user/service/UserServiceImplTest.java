package com.microservice.user.service;

import com.microservice.user.dto.UserDto;
import com.microservice.user.mapper.UserMapper;
import com.microservice.user.mapper.UserMapperImpl;
import com.microservice.user.model.User;
import com.microservice.user.repository.UserRepository;
import com.microservice.user.service.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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
        Mockito.when(userRepository.save(ArgumentMatchers.any())).thenReturn(Mono.just(new User()));

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
