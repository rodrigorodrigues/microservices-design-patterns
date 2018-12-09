package com.learning.springboot.controller;

import com.learning.springboot.dto.PersonDto;
import com.learning.springboot.mapper.PersonMapper;
import com.learning.springboot.model.Person;
import com.learning.springboot.service.PersonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;
import java.util.List;

/**
 * Rest API
 */
@RestController
@Api(value = "persons", description = "Methods for managing persons")
@RequestMapping("/persons")
@AllArgsConstructor
public
class PersonController {
    private final PersonService personService;

    private final PasswordEncoder passwordEncoder;

    private final PersonMapper personMapper;

    @ApiOperation(value = "Api for return list of persons")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'READ')")
    public ResponseEntity<List<PersonDto>> findAll() {
        return ResponseEntity.ok(personMapper.entityToDto(personService.findAll()));
    }

    @ApiOperation(value = "Api for return a person by id")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'READ')")
    public ResponseEntity<PersonDto> findById(@ApiParam(required = true) @PathVariable String id) {
        return personService.findById(id)
            .map(p -> ResponseEntity.ok(personMapper.entityToDto(p)))
            .orElse(ResponseEntity.notFound().build());
    }

    @ApiOperation(value = "Api for creating a person")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'CREATE')")
    public ResponseEntity<PersonDto> create(@RequestBody @Valid @ApiParam(required = true) PersonDto person) {
        person.setPassword(passwordEncoder.encode(person.getPassword()));
        Person personModel = personService.save(personMapper.dtoToEntity(person));
        person.setId(personModel.getId());

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath().path("/{id}")
                .buildAndExpand(person.getId())
                .toUri();
        return ResponseEntity.created(location).body(person);
    }

    @ApiOperation(value = "Api for updating a person")
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SAVE')")
    public ResponseEntity<PersonDto> update(@RequestBody @Valid @ApiParam(required = true) PersonDto person,
                                                   @PathVariable @ApiParam(required = true) String id) {
        return personService.findById(id)
                .map(p -> {
                    person.setId(id);
                    personService.save(personMapper.dtoToEntity(person));
                    return ResponseEntity.ok(person);
                }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @ApiOperation(value = "Api for deleting a person")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELETE')")
    public void delete(@PathVariable @ApiParam(required = true) String id) {
        personService.deleteById(id);
    }
}
