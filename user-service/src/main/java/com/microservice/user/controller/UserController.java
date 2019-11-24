package com.microservice.user.controller;

import com.microservice.user.config.SpringSecurityAuditorAware;
import com.microservice.user.dto.UserDto;
import com.microservice.user.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import springfox.documentation.annotations.ApiIgnore;

import java.net.URI;

/**
 * Rest API for users.
 */
@Slf4j
@RestController
@Api(value = "users", description = "Methods for managing users")
@RequestMapping("/api/users")
@AllArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    private final UserService userService;

    private final SpringSecurityAuditorAware springSecurityAuditorAware;

    @ApiOperation(value = "Api for return list of users")
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<UserDto> findAll(@ApiIgnore @AuthenticationPrincipal Authentication authentication) {
        if (authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"))) {
            return userService.findAll();
        } else {
            return userService.findAll()
                    .filter(p -> p.getCreatedByUser().equals(authentication.getName()))
                    .collectList()
                    .flatMapMany(Flux::fromIterable);
        }
    }

    @ApiOperation(value = "Api for return a user by id")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<UserDto> findById(@ApiParam(required = true) @PathVariable String id) {
        return userService.findById(id)
            .switchIfEmpty(responseNotFound());
    }

    @ApiOperation(value = "Api for creating a user")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<UserDto>> create(@RequestBody @ApiParam(required = true) UserDto user,
                                                @ApiIgnore @AuthenticationPrincipal Authentication authentication) {
        springSecurityAuditorAware.setCurrentAuthenticatedUser(authentication);
        return userService.save(user)
                .map(p -> ResponseEntity.created(URI.create(String.format("/api/users/%s", p.getId())))
                        .body(p));
    }

    @ApiOperation(value = "Api for updating a user")
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<UserDto> update(@RequestBody @ApiParam(required = true) UserDto user,
                                @PathVariable @ApiParam(required = true) String id,
                                @ApiIgnore @AuthenticationPrincipal Authentication authentication) {
        springSecurityAuditorAware.setCurrentAuthenticatedUser(authentication);
        user.setId(id);
        return userService.findById(id)
                .switchIfEmpty(responseNotFound())
                .flatMap(u -> userService.save(user));
    }

    @ApiOperation(value = "Api for deleting a user")
    @DeleteMapping("/{id}")
    public Mono<Void> delete(@PathVariable @ApiParam(required = true) String id) {
        return userService.findById(id)
                .switchIfEmpty(responseNotFound())
                .flatMap(u -> userService.deleteById(id));
    }

    private Mono<UserDto> responseNotFound() {
        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
