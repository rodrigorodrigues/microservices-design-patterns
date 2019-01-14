package com.learning.springboot.service;

import com.learning.springboot.dto.UserDto;
import com.learning.springboot.mapper.UserMapper;
import com.learning.springboot.model.User;
import com.learning.springboot.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<UserDto> save(UserDto userDto) {
        if (StringUtils.isBlank(userDto.getId())) {
            if (!StringUtils.equals(userDto.getPassword(), userDto.getConfirmPassword()) || StringUtils.isBlank(userDto.getConfirmPassword())) {
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirm password is different than password!"));
            }
            userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));
        }
        User user = userMapper.dtoToEntity(userDto);
        return userMapper.entityToDto(userRepository.save(user));
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

    @Override
    public Mono<UserDto> findByEmail(String email) {
        return userMapper.entityToDto(userRepository.findByEmail(email));
    }

    @Override
    public Mono<User> findSystemDefaultUser() {
        return userRepository.findByEmail("default@admin.com");
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException(String.format("User(%s) not found!", username))))
                .map(p -> p);
    }
}
