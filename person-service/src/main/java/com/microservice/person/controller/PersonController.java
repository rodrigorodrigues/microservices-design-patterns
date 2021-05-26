package com.microservice.person.controller;

import java.net.URI;
import java.util.Collection;

import com.microservice.person.dto.PersonDto;
import com.microservice.person.model.Person;
import com.microservice.person.repository.PersonRepository;
import com.microservice.person.service.PersonService;
import com.querydsl.core.types.Predicate;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import springfox.documentation.annotations.ApiIgnore;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.CollectionUtils;
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
 * Rest API for persons.
 */
@Slf4j
@RestController
@Api(value = "persons", description = "Methods for managing persons")
@RequestMapping("/api/people")
@AllArgsConstructor
public class PersonController {
    private final PersonService personService;

    @ApiOperation(value = "Api for return list of persons")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'PERSON_READ', 'PERSON_SAVE', 'PERSON_DELETE', 'PERSON_CREATE') or hasAuthority('SCOPE_openid')")
    public ResponseEntity<Page<PersonDto>> findAll(@ApiIgnore Authentication authentication,
                                                   @RequestParam(name = "page", defaultValue = "0", required = false) Integer page,
                                                   @RequestParam(name = "size", defaultValue = "10", required = false) Integer size,
                                                   @RequestParam(name = "sort-dir", defaultValue = "desc", required = false) String sortDirection,
                                                   @RequestParam(name = "sort-idx", defaultValue = "createdDate", required = false) String[] sortIdx,
                                                   @QuerydslPredicate(root = Person.class, bindings = PersonRepository.class) Predicate predicate) {
        log.debug("predicate: {}", predicate);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortIdx));
        if (hasRoleAdmin(authentication)) {
            return ResponseEntity.ok(personService.findAll(pageRequest, predicate));
        } else {
            return ResponseEntity.ok(personService.findAllByCreatedByUser(authentication.getName(), pageRequest, predicate));
        }
    }

    @ApiOperation(value = "Api for return a person by id")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'PERSON_READ', 'PERSON_SAVE') or hasAuthority('SCOPE_openid')")
    public ResponseEntity<PersonDto> findById(@ApiParam(required = true) @PathVariable String id,
                                    @ApiIgnore Authentication authentication) {
        PersonDto personServiceById = personService.findById(id);
        if (personServiceById == null) {
            throw responseNotFound();
        } else if (hasRoleAdmin(authentication) || authentication.getName().equals(personServiceById.getCreatedByUser())) {
            return ResponseEntity.ok(personServiceById);
        } else {
            throw new AccessDeniedException(String.format("User(%s) does not have access to this resource", authentication.getName()));
        }
    }

    @ApiOperation(value = "Api for creating a person")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'PERSON_CREATE') or hasAuthority('SCOPE_openid')")
    public ResponseEntity<PersonDto> create(@RequestBody @ApiParam(required = true) PersonDto person) {
        PersonDto save = personService.save(person);
        return ResponseEntity.created(URI.create(String.format("/api/people/%s", save.getId()))).body(save);
    }

    @ApiOperation(value = "Api for updating a person")
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("(hasAnyRole('ADMIN', 'PERSON_SAVE') or hasAuthority('SCOPE_openid')) and (hasRole('ADMIN') or #person.createdByUser == authentication.name)")
    public ResponseEntity<PersonDto> update(@RequestBody @ApiParam(required = true) PersonDto person,
                                  @PathVariable @ApiParam(required = true) String id) {
        person.setId(id);
        PersonDto personServiceById = personService.findById(id);
        if (personServiceById == null) {
            throw responseNotFound();
        } else {
            return ResponseEntity.ok(personService.save(person));
        }
    }

    @ApiOperation(value = "Api for deleting a person")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PERSON_DELETE') or hasAuthority('SCOPE_openid')")
    public void delete(@PathVariable @ApiParam(required = true) String id,
                             @ApiIgnore Authentication authentication) {
        PersonDto personServiceById = personService.findById(id);
        if (personServiceById == null) {
            throw responseNotFound();
        } else if (hasRoleAdmin(authentication) || personServiceById.getCreatedByUser().equals(authentication.getName())) {
            personService.deleteById(id);
        } else {
            throw new AccessDeniedException(String.format("User(%s) does not have access to delete this resource", authentication.getName()));
        }
    }

    private boolean hasRoleAdmin(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return !CollectionUtils.isEmpty(authorities) && authorities.stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"));
    }

    private ResponseStatusException responseNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

}
