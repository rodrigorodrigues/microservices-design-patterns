package com.microservice.user.controller;

import com.microservice.user.dto.UserDto;
import com.microservice.user.model.User;
import com.microservice.user.repository.UserRepository;
import com.microservice.user.service.UserService;
import com.querydsl.core.types.Predicate;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
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

    @ApiOperation(value = "Api for return list of users")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<UserDto>> findAll(@ApiIgnore @AuthenticationPrincipal Authentication authentication,
                                 @RequestParam(name = "page", defaultValue = "0", required = false) Integer page,
                                 @RequestParam(name = "size", defaultValue = "10", required = false) Integer size,
                                 @RequestParam(name = "sort-dir", defaultValue = "desc", required = false) String sortDirection,
                                 @RequestParam(name = "sort-idx", defaultValue = "createdDate", required = false) String[] sortIdx,
                                 @QuerydslPredicate(root = User.class, bindings = UserRepository.class) Predicate predicate) {
        log.debug("predicate: {}", predicate);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortIdx));
        if (authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"))) {
            return ResponseEntity.ok(userService.findAll(pageRequest, predicate));
        } else {
            return ResponseEntity.ok(userService.findAllByCreatedByUser(authentication.getName(), pageRequest, predicate));
        }
    }

    @ApiOperation(value = "Api for return a user by id")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserDto> findById(@ApiParam(required = true) @PathVariable String id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @ApiOperation(value = "Api for creating a user")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserDto> create(@RequestBody @ApiParam(required = true) UserDto user) {
        UserDto save = userService.save(user);
        return ResponseEntity.created(URI.create(String.format("/api/users/%s", save.getId()))).body(save);
    }

    @ApiOperation(value = "Api for updating a user")
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserDto> update(@RequestBody @ApiParam(required = true) UserDto user,
                                @PathVariable @ApiParam(required = true) String id) {
        user.setId(id);
        UserDto userServiceById = userService.findById(id);
        if (userServiceById == null) {
            throw responseNotFound();
        } else {
            return ResponseEntity.ok(userService.save(user));
        }
    }

    @ApiOperation(value = "Api for deleting a user")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable @ApiParam(required = true) String id) {
        UserDto userServiceById = userService.findById(id);
        if (userServiceById == null) {
            throw responseNotFound();
        } else {
            userService.deleteById(id);
        }
    }

    private ResponseStatusException responseNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
