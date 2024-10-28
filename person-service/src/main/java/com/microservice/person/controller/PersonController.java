package com.microservice.person.controller;

import java.net.URI;
import java.util.Collection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microservice.person.dto.PersonDto;
import com.microservice.person.model.Person;
import com.microservice.person.repository.PersonRepository;
import com.microservice.person.service.PersonService;
import com.querydsl.core.types.Predicate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Rest API for persons.
 */
@Slf4j
@RestController
@Tag(name = "persons", description = "Methods for managing persons")
@RequestMapping("/api/people")
@AllArgsConstructor
public class PersonController {
    private final PersonService personService;

    /*@Operation(description = "Api for return list of persons")
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Page<PersonDto>> findAllByName(@Parameter(hidden = true) Authentication authentication,
        @RequestParam(name = "page", defaultValue = "0", required = false) Integer page,
        @RequestParam(name = "size", defaultValue = "10", required = false) Integer size,
        @RequestParam(name = "sort-dir", defaultValue = "desc", required = false) String sortDirection) {

    }*/

    @Operation(description = "Api for return list of persons", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'PERSON_READ', 'PERSON_SAVE', 'PERSON_DELETE', 'PERSON_CREATE') or hasAnyAuthority('SCOPE_openid', 'client.create')")
    public ResponseEntity<Page<PersonDto>> findAll(@Parameter(hidden = true) Authentication authentication,
                                                   @RequestParam(name = "page", defaultValue = "0", required = false) Integer page,
                                                   @RequestParam(name = "size", defaultValue = "10", required = false) Integer size,
                                                   @RequestParam(name = "sort-dir", defaultValue = "desc", required = false) String sortDirection,
                                                   @RequestParam(name = "sort-idx", defaultValue = "createdDate", required = false) String[] sortIdx,
                                                   @Parameter(hidden = true) @QuerydslPredicate(root = Person.class, bindings = PersonRepository.class) Predicate predicate,
                                                   @Parameter(hidden = true) @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {

        log.debug("predicate: {}", predicate);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortIdx));
        if (hasRoleAdmin(authentication)) {
            return ResponseEntity.ok(personService.findAll(pageRequest, predicate, authorization));
        } else {
            return ResponseEntity.ok(personService.findAllByCreatedByUser(authentication.getName(), pageRequest, predicate, authorization));
        }
    }

    @Operation(description = "Api for return a person by id", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'PERSON_READ', 'PERSON_SAVE') or hasAuthority('SCOPE_openid')")
    public ResponseEntity<PersonDto> findById(@Parameter(required = true) @PathVariable String id,
        @Parameter(hidden = true) Authentication authentication) {
        PersonDto personServiceById = personService.findById(id);
        if (personServiceById == null) {
            throw responseNotFound();
        } else if (hasRoleAdmin(authentication) || authentication.getName().equals(personServiceById.getCreatedByUser())) {
            return ResponseEntity.ok(personServiceById);
        } else {
            throw new AccessDeniedException(String.format("User(%s) does not have access to this resource", authentication.getName()));
        }
    }

    @Operation(description = "Api for creating a person", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'PERSON_CREATE') or hasAuthority('SCOPE_openid')")
    public ResponseEntity<PersonDto> create(@RequestBody @Parameter(required = true) PersonDto person) throws JsonProcessingException {
        PersonDto save = personService.save(person);
        return ResponseEntity.created(URI.create(String.format("/api/people/%s", save.getId()))).body(save);
    }

    @Operation(description = "Api for updating a person", security = { @SecurityRequirement(name = "bearer-key") })
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("(hasAnyRole('ADMIN', 'PERSON_SAVE') or hasAuthority('SCOPE_openid')) and (hasRole('ADMIN') or #person.createdByUser == authentication.name)")
    public ResponseEntity<PersonDto> update(@RequestBody @Parameter(required = true) PersonDto person,
                                  @PathVariable @Parameter(required = true) String id) {
        person.setId(id);
        PersonDto personServiceById = personService.findById(id);
        if (personServiceById == null) {
            throw responseNotFound();
        } else {
            return ResponseEntity.ok(personService.save(person));
        }
    }

    @Operation(description = "Api for deleting a person", security = { @SecurityRequirement(name = "bearer-key") })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PERSON_DELETE') or hasAuthority('SCOPE_openid')")
    public void delete(@PathVariable @Parameter(required = true) String id,
        @Parameter(hidden = true) Authentication authentication) {
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
