package com.microservice.user.service;

import com.microservice.user.dto.UserDto;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserDto save(UserDto userDto);
    UserDto findById(String id);
    Page<UserDto> findAll(Pageable pageable, Predicate predicate);
    Page<UserDto> findAllByCreatedByUser(String createdByUser, Pageable pageable, Predicate predicate);
    void deleteById(String id);
}
