package com.microservice.user.mapper;

import com.microservice.user.dto.UserDto;
import com.microservice.user.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.support.PageableExecutionUtils;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    default Page<UserDto> entityToDto(Page<User> users, long count) {
        return PageableExecutionUtils.getPage(entityToDto(users.getContent()), users.getPageable(), () -> count);
    }

    List<UserDto> entityToDto(List<User> users);

    User dtoToEntity(UserDto userDto);

    @Mapping(source = "enabled", target = "activated", defaultValue = "true")
    UserDto entityToDto(User user);
}
