package com.microservice.person.controller;

import com.microservice.person.config.SpringSecurityAuditorAware;
import com.microservice.person.dto.PersonDto;
import com.microservice.person.service.PersonService;
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

    private final SpringSecurityAuditorAware springSecurityAuditorAware;

    /**
    @ApiOperation(value = "Api for return list of persons")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PERSON_READ')")
    public Flux<ServerSentEvent<PersonDto>> findAll(@RequestHeader(value = "Last-Event-ID", required = false) String lastEventIdHeader,
                                                    @RequestParam(value = "lastEventId", required = false) String lastEventIdRequestParam) {
        log.debug("findAll:lastEventIdHeader: {}\tlastEventIdRequestParam: {}", lastEventIdHeader, lastEventIdRequestParam);
        Integer lastEventId = generateLastEventId(lastEventIdHeader, lastEventIdRequestParam);
        return personService.findAll()
                .map(p -> {
                    log.debug("findAll:person: {}", p);
                    ServerSentEvent<PersonDto> message = ServerSentEvent.<PersonDto>builder()
                            .event("message")
                            .data(p)
                            .id(String.valueOf(lastEventId))
                            .build();
                    log.debug("Message: {}", message);
                    return message;
                });
    }
*/
    @ApiOperation(value = "Api for return list of persons")
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'PERSON_READ', 'PERSON_SAVE', 'PERSON_DELETE', 'PERSON_CREATE')")
    public Flux<PersonDto> findAll(@AuthenticationPrincipal Authentication authentication) {
        if (hasRoleAdmin(authentication)) {
            return personService.findAll();
        } else {
            return personService.findAll()
                .filter(p -> p.getCreatedByUser().equals(authentication.getName()))
                .collectList()
                .flatMapMany(Flux::fromIterable);
        }
    }

/*
    private Integer generateLastEventId(String lastEventIdHeader, String lastEventIdRequestParam) {
        Integer lastEventId = 0;
        try {
            if (StringUtils.isNotBlank(lastEventIdHeader)) {
                lastEventId = Integer.parseInt(lastEventIdHeader);
            } else if (StringUtils.isNotBlank(lastEventIdRequestParam)) {
                lastEventId = Integer.parseInt(lastEventIdRequestParam);
            }
        } catch (Exception e) {
            log.error("Invalid last event", e);
        }
        return lastEventId;
    }
*/

    @ApiOperation(value = "Api for return a person by id")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'PERSON_READ', 'PERSON_SAVE')")
    public Mono<PersonDto> findById(@ApiParam(required = true) @PathVariable String id,
                                    @AuthenticationPrincipal Authentication authentication) {
        return personService.findById(id)
            .flatMap(p -> {
                if (hasRoleAdmin(authentication) || p.getCreatedByUser().equals(authentication.getName())) {
                    return Mono.just(p);
                } else {
                    return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, String.format("User(%s) does not have access to this resource", authentication.getName())));
                }
            })
            .switchIfEmpty(responseNotFound());
    }

    @ApiOperation(value = "Api for creating a person")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'PERSON_CREATE')")
    public Mono<ResponseEntity<PersonDto>> create(@RequestBody @ApiParam(required = true) PersonDto person,
                                                  @AuthenticationPrincipal Authentication authentication) {
        springSecurityAuditorAware.setCurrentAuthenticatedUser(authentication);
        return personService.save(person)
                .map(p -> ResponseEntity.created(URI.create(String.format("/api/persons/%s", p.getId())))
                        .body(p));
    }

    @ApiOperation(value = "Api for updating a person")
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'PERSON_SAVE')")
    public Mono<PersonDto> update(@RequestBody @ApiParam(required = true) PersonDto person,
                                  @PathVariable @ApiParam(required = true) String id,
                                  @AuthenticationPrincipal Authentication authentication) {
        springSecurityAuditorAware.setCurrentAuthenticatedUser(authentication);
        person.setId(id);
        return personService.findById(id)
                .switchIfEmpty(responseNotFound())
                .flatMap(p -> personService.save(person));
    }

    @ApiOperation(value = "Api for deleting a person")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PERSON_DELETE')")
    public Mono<Void> delete(@PathVariable @ApiParam(required = true) String id) {
        return personService.findById(id)
            .switchIfEmpty(responseNotFound())
            .flatMap(u -> personService.deleteById(id));
    }

    private boolean hasRoleAdmin(@AuthenticationPrincipal Authentication authentication) {
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"));
    }

    private Mono<PersonDto> responseNotFound() {
        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

}
