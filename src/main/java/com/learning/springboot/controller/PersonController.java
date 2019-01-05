package com.learning.springboot.controller;

import com.learning.springboot.dto.PersonDto;
import com.learning.springboot.service.PersonService;
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
 * Rest API for persons.
 */
@Slf4j
@RestController
@Api(value = "persons", description = "Methods for managing persons")
@RequestMapping("/api/persons")
@AllArgsConstructor
public class PersonController {
    private final PersonService personService;

    @ApiOperation(value = "Api for return list of persons")
    @GetMapping(produces = { MediaType.APPLICATION_STREAM_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE })
    @PreAuthorize("hasAnyRole('ADMIN', 'READ')")
    public Flux<PersonDto> findAll() {
        return personService.findAll();
    }

    @ApiOperation(value = "Api for return a person by id")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'READ')")
    public Mono<PersonDto> findById(@ApiParam(required = true) @PathVariable String id) {
        return personService.findById(id)
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    @ApiOperation(value = "Api for creating a person")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'CREATE')")
    public Mono<ResponseEntity<PersonDto>> create(@RequestBody @ApiParam(required = true) PersonDto person) {
        return personService.save(person)
                .map(p -> ResponseEntity.created(URI.create(String.format("/api/persons/%s", p.getId())))
                        .body(p));
    }

    @ApiOperation(value = "Api for updating a person")
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SAVE')")
    public Mono<PersonDto> update(@RequestBody @ApiParam(required = true) PersonDto person,
                                                   @PathVariable @ApiParam(required = true) String id) {
        person.setId(id);
        return personService.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                .flatMap(p -> personService.save(p));
    }

    @ApiOperation(value = "Api for deleting a person")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELETE')")
    public Mono<Void> delete(@PathVariable @ApiParam(required = true) String id) {
        return personService.deleteById(id);
    }
}
