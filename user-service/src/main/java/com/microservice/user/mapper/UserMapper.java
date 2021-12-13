package com.microservice.user.mapper;

import java.util.List;

import com.microservice.user.dto.UserDto;
import com.microservice.user.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import org.springframework.data.domain.Page;
import org.springframework.data.support.PageableExecutionUtils;

@Mapper(componentModel = "spring")
public interface UserMapper {
    default Page<UserDto> entityToDto(Page<User> users, long count) {
        return PageableExecutionUtils.getPage(entityToDto(users.getContent()), users.getPageable(), () -> count);
    }

    List<UserDto> entityToDto(List<User> users);

    User dtoToEntity(UserDto userDto);

    @Mapping(source = "enabled", target = "activated", defaultValue = "true")
    @Mapping(source = "password", target = "password", ignore = true)
    UserDto entityToDto(User user);
}
