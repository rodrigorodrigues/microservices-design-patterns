package com.microservice.person.controller;

import com.microservice.person.config.SpringSecurityAuditorAware;
import com.microservice.person.dto.PersonDto;
import com.microservice.person.model.QPerson;
import com.microservice.person.service.PersonService;
import com.querydsl.core.types.dsl.BooleanExpression;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostFilter;
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
 * Rest API for persons.
 */
@Slf4j
@RestController
@Api(value = "persons", description = "Methods for managing persons")
@RequestMapping("/api/people")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'PERSON_READ', 'PERSON_SAVE', 'PERSON_DELETE', 'PERSON_CREATE') or hasAuthority('SCOPE_openid')")
    public Flux<PersonDto> findAll(@AuthenticationPrincipal Authentication authentication,
                              @RequestParam(name = "page", defaultValue = "0", required = false) Integer page,
                              @RequestParam(name = "size", defaultValue = "10", required = false) Integer size,
                              @RequestParam(name = "sort-dir", defaultValue = "desc", required = false) String sortDirection,
                              @RequestParam(name = "sort-idx", defaultValue = "createdDate", required = false) String[] sortIdx,
                              @RequestParam(required = false) String search) {
        BooleanExpression predicate = QPerson.person.id.isNotNull();
        if (StringUtils.isNotBlank(search)) {
            for (String token : search.split(";")) {
                if (StringUtils.containsIgnoreCase(token, "address:")) {
                    predicate = QPerson.person.address.address.containsIgnoreCase(token.replaceFirst("(?i)address:", ""));
                }
            }
        }
        log.info("predicate: {}", predicate);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortIdx));
        if (hasRoleAdmin(authentication)) {
            return personService.findAll(pageRequest, predicate);
        } else {
            return personService.findAllByCreatedByUser(authentication.getName(), pageRequest);
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
    @PreAuthorize("hasAnyRole('ADMIN', 'PERSON_READ', 'PERSON_SAVE') or hasAuthority('SCOPE_openid')")
    public Mono<PersonDto> findById(@ApiParam(required = true) @PathVariable String id,
                                    @ApiIgnore @AuthenticationPrincipal Authentication authentication) {
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
    @PreAuthorize("hasAnyRole('ADMIN', 'PERSON_CREATE') or hasAuthority('SCOPE_openid')")
    public Mono<ResponseEntity<PersonDto>> create(@RequestBody @ApiParam(required = true) PersonDto person,
                                                  @ApiIgnore @AuthenticationPrincipal Authentication authentication) {
        springSecurityAuditorAware.setCurrentAuthenticatedUser(authentication);
        return personService.save(person)
                .map(p -> ResponseEntity.created(URI.create(String.format("/api/people/%s", p.getId())))
                        .body(p));
    }

    @ApiOperation(value = "Api for updating a person")
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'PERSON_SAVE') or hasAuthority('SCOPE_openid') and (hasRole('ADMIN') or #person.createdByUser == authentication.name)")
    public Mono<PersonDto> update(@RequestBody @ApiParam(required = true) PersonDto person,
                                  @PathVariable @ApiParam(required = true) String id,
                                  @ApiIgnore @AuthenticationPrincipal Authentication authentication) {
        springSecurityAuditorAware.setCurrentAuthenticatedUser(authentication);
        person.setId(id);
        return personService.findById(id)
                .switchIfEmpty(responseNotFound())
                .flatMap(p -> personService.save(person));
    }

    @ApiOperation(value = "Api for deleting a person")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PERSON_DELETE') or hasAuthority('SCOPE_openid')")
    public Mono<Void> delete(@PathVariable @ApiParam(required = true) String id,
                             @ApiIgnore @AuthenticationPrincipal Authentication authentication) {
        return personService.findById(id)
            .switchIfEmpty(responseNotFound())
            .flatMap(u -> {
                if (hasRoleAdmin(authentication) || u.getCreatedByUser().equals(authentication.getName())) {
                    return personService.deleteById(id);
                } else {
                    return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, String.format("User(%s) does not have access to delete this resource", authentication.getName())));
                }
            });
    }

    private boolean hasRoleAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"));
    }

    private Mono<PersonDto> responseNotFound() {
        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

}
