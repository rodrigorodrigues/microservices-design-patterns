package com.microservice.user.service;

import com.microservice.user.dto.UserDto;
import com.microservice.user.mapper.UserMapper;
import com.microservice.user.mapper.UserMapperImpl;
import com.microservice.user.model.User;
import com.microservice.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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
        when(userRepository.save(any())).thenReturn(new User());

        UserDto user = userService.save(userDto);

        assertThat(user).isNotNull();
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

        when(userRepository.findById(anyString())).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        UserDto save = userService.save(userDto);
        assertThat(save).isNotNull();
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
        when(userRepository.findById(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        Exception exception = assertThrows(ResponseStatusException.class, () -> userService.save(userDto));
        assertThat(exception.getLocalizedMessage()).contains("Current password is incorrect!");
    }

   @Test
    public void whenCallSaveShouldThrowExceptionIfConfirmPasswordIsNotPresent() {
        UserDto userDto = new UserDto();

       Exception exception = assertThrows(ResponseStatusException.class, () -> userService.save(userDto));
       assertThat(exception.getLocalizedMessage()).contains("Password must not be null!");
    }

    @Test
    public void whenCallSaveShouldThrowExceptionIfConfirmPasswordIsDifferentThanPassword() {
        UserDto userDto = UserDto.builder()
            .password("123")
            .confirmPassword("423").build();

        Exception exception = assertThrows(ResponseStatusException.class, () -> userService.save(userDto));
        assertThat(exception.getLocalizedMessage()).contains("Confirm password is different than password!");
    }
}
