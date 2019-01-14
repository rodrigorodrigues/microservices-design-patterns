package com.learning.springboot.controller;

import com.learning.springboot.dto.UserDto;
import com.learning.springboot.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Rest API for users.
 */
@Slf4j
@RestController
@Api(value = "users", description = "Methods for managing users")
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserController {
    private final UserService userService;

    @ApiOperation(value = "Api for return list of users")
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'READ')")
    public Flux<UserDto> findAll() {
        return userService.findAll();
    }

    @ApiOperation(value = "Api for return a user by id")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'READ')")
    public Mono<UserDto> findById(@ApiParam(required = true) @PathVariable String id) {
        return userService.findById(id)
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    @ApiOperation(value = "Api for creating a user")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'CREATE')")
    public Mono<ResponseEntity<UserDto>> create(@RequestBody @ApiParam(required = true) UserDto user) {
        return userService.save(user)
                .map(p -> ResponseEntity.created(URI.create(String.format("/api/users/%s", p.getId())))
                        .body(p));
    }

    @ApiOperation(value = "Api for updating a user")
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SAVE')")
    public Mono<UserDto> update(@RequestBody @ApiParam(required = true) UserDto user,
                                                   @PathVariable @ApiParam(required = true) String id) {
        user.setId(id);
        return userService.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                .flatMap(userService::save);
    }

    @ApiOperation(value = "Api for deleting a user")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELETE')")
    public Mono<Void> delete(@PathVariable @ApiParam(required = true) String id) {
        return userService.deleteById(id);
    }
}
