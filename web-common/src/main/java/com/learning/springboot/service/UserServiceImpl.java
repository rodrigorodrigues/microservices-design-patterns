package com.learning.springboot.service;

import com.learning.springboot.dto.UserDto;
import com.learning.springboot.mapper.UserMapper;
import com.learning.springboot.model.User;
import com.learning.springboot.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    private final UserMapper userMapper;

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
