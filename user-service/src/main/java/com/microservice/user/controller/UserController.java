package com.microservice.user.controller;

import java.net.URI;

import com.microservice.user.dto.UserDto;
import com.microservice.user.model.User;
import com.microservice.user.repository.UserRepository;
import com.microservice.user.service.UserService;
import com.querydsl.core.types.Predicate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Rest API for users.
 */
@Slf4j
@RestController
@Tag(name = "users", description = "Methods for managing users")
@RequestMapping("/api/users")
@AllArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasAnyAuthority('ADMIN')")
public class UserController {
    private final UserService userService;

    @Operation(description = "Api for return list of users")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<UserDto>> findAll(@Parameter(hidden = true) Authentication authentication,
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

    @Operation(description = "Api for return a user by id")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserDto> findById(@Parameter(required = true) @PathVariable String id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @Operation(description = "Api for creating a user")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserDto> create(@RequestBody @Parameter(required = true) UserDto user) {
        UserDto save = userService.save(user);
        return ResponseEntity.created(URI.create(String.format("/api/users/%s", save.getId()))).body(save);
    }

    @Operation(description = "Api for updating a user")
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserDto> update(@RequestBody @Parameter(required = true) UserDto user,
                                @PathVariable @Parameter(required = true) String id) {
        user.setId(id);
        UserDto userServiceById = userService.findById(id);
        if (userServiceById == null) {
            throw responseNotFound();
        } else {
            return ResponseEntity.ok(userService.save(user));
        }
    }

    @Operation(description = "Api for deleting a user")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable @Parameter(required = true) String id) {
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
