package com.microservice.user.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.microservice.user.dto.UserDto;
import com.microservice.user.mapper.UserMapper;
import com.microservice.user.mapper.UserMapperImpl;
import com.microservice.user.model.User;
import com.microservice.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
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
        when(userRepository.save(any())).thenReturn(Mono.just(new User()));

        StepVerifier.create(userService.save(userDto))
                .expectNextCount(1)
                .verifyComplete();

        verify(userRepository, never()).findById(anyString());
    }

    @Test
    void whenCallSaveShouldUpdateUser() {
        UserDto userDto = UserDto.builder()
            .id(UUID.randomUUID().toString())
            .currentPassword("123")
            .password("123")
            .confirmPassword("123").build();
        User user = new User();
        user.setPassword("123");
        Mono<User> userMono = Mono.just(user);
        when(userRepository.findById(anyString())).thenReturn(userMono);
        when(userRepository.save(any())).thenReturn(userMono);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        StepVerifier.create(userService.save(userDto))
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void whenCallSaveShouldUpdateUserShouldThrowExceptionWhenCurrentPasswordDoesNotMatch() {
        UserDto userDto = UserDto.builder()
            .id(UUID.randomUUID().toString())
            .currentPassword("123")
            .password("123")
            .confirmPassword("123").build();
        User user = new User();
        user.setPassword("123");
        when(userRepository.findById(anyString())).thenReturn(Mono.just(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        StepVerifier.create(userService.save(userDto))
            .expectError(ResponseStatusException.class)
            .verifyThenAssertThat()
            .hasOperatorErrorWithMessageContaining("Current password is incorrect!");
    }

   @Test
    public void whenCallSaveShouldThrowExceptionIfConfirmPasswordIsNotPresent() {
        UserDto userDto = new UserDto();
        StepVerifier.create(userService.save(userDto))
                .expectError(ResponseStatusException.class)
                .verifyThenAssertThat()
                .hasOperatorErrorWithMessageContaining("Password must not be null!");
    }

    @Test
    public void whenCallSaveShouldThrowExceptionIfConfirmPasswordIsDifferentThanPassword() {
        UserDto userDto = UserDto.builder()
            .password("123")
            .confirmPassword("423").build();

        StepVerifier.create(userService.save(userDto))
            .expectError(ResponseStatusException.class)
            .verifyThenAssertThat()
            .hasOperatorErrorWithMessageContaining("Confirm password is different than password!");
    }
}
