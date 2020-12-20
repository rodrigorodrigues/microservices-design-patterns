package com.microservice.user.service;

import com.microservice.user.dto.UserDto;
import com.microservice.user.mapper.UserMapper;
import com.microservice.user.model.User;
import com.microservice.user.repository.UserRepository;
import com.querydsl.core.types.Predicate;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDto save(UserDto userDto) {
        boolean newUser = StringUtils.isBlank(userDto.getId());
        if (newUser) {
            if (StringUtils.isBlank(userDto.getPassword()) || StringUtils.isBlank(userDto.getConfirmPassword())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must not be null!");
            }

            if (!StringUtils.equals(userDto.getPassword(), userDto.getConfirmPassword())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirm password is different than password!");
            }
        }
        User user = userMapper.dtoToEntity(userDto);
        if (newUser) {
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
            return userMapper.entityToDto(userRepository.save(user));
        } else {
            return userRepository.findById(userDto.getId())
                .map(u -> {
                    if (StringUtils.isNotBlank(userDto.getPassword())) {
                        if (!passwordEncoder.matches(userDto.getCurrentPassword(), u.getPassword())) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect!");
                        }

                        if (!StringUtils.equals(userDto.getPassword(), userDto.getConfirmPassword()))  {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirm password is different than password!");
                        }

                        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
                    } else {
                        user.setPassword(u.getPassword());
                    }
                    return userMapper.entityToDto(userRepository.save(user));
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        }
    }

    @Override
    public UserDto findById(String id) {
        return userRepository.findById(id)
            .map(userMapper::entityToDto)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Override
    public Page<UserDto> findAll(Pageable pageable, Predicate predicate) {
        return userMapper.entityToDto(userRepository.findAll(predicate, pageable), userRepository.count(predicate));
    }

    @Override
    public Page<UserDto> findAllByCreatedByUser(String createdByUser, Pageable pageable) {
        return userMapper.entityToDto(userRepository.findAllByCreatedByUser(createdByUser, pageable), userRepository.count());
    }

    @Override
    public void deleteById(String id) {
        userRepository.deleteById(id);
    }

}
