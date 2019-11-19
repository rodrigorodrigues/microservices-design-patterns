package com.microservice.user.service;

import com.microservice.user.dto.UserDto;
import com.microservice.user.mapper.UserMapper;
import com.microservice.user.model.User;
import com.microservice.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<UserDto> save(UserDto userDto) {
        boolean newUser = StringUtils.isBlank(userDto.getId());
        if (newUser) {
            if (StringUtils.isBlank(userDto.getPassword()) || StringUtils.isBlank(userDto.getConfirmPassword())) {
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must not be null!"));
            }

            if (!StringUtils.equals(userDto.getPassword(), userDto.getConfirmPassword())) {
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirm password is different than password!"));
            }
        }
        User user = userMapper.dtoToEntity(userDto);
        if (newUser) {
            userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));
            return userMapper.entityToDto(userRepository.save(user));
        } else {
            return userRepository.findById(userDto.getId())
                    .flatMap(u -> {
                        if (StringUtils.isNotBlank(userDto.getPassword())) {
                            if (!passwordEncoder.matches(userDto.getCurrentPassword(), u.getPassword())) {
                                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect!"));
                            }

                            if (!StringUtils.equals(userDto.getPassword(), userDto.getConfirmPassword()))  {
                                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirm password is different than password!"));
                            }

                            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
                        } else {
                            user.setPassword(u.getPassword());
                        }
                        return userMapper.entityToDto(userRepository.save(user));
                    });
        }
    }

    @Override
    public Mono<UserDto> findById(String id) {
        return userMapper.entityToDto(userRepository.findById(id));
    }

    @Override
    public Flux<UserDto> findAll() {
        return userMapper.entityToDto(userRepository.findAll());
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return userRepository.deleteById(id);
    }

}
