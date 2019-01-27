package com.learning.springboot.controller;

import com.learning.springboot.dto.PersonDto;
import com.learning.springboot.service.PersonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

    /**
    @ApiOperation(value = "Api for return list of persons")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'READ')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'READ', 'SAVE', 'DELETE', 'CREATE')")
    public Flux<PersonDto> findAll() {
        return personService.findAll();
    }

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

    @ApiOperation(value = "Api for return a person by id")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'READ', 'SAVE')")
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
                .flatMap(personService::save);
    }

    @ApiOperation(value = "Api for deleting a person")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELETE')")
    public Mono<Void> delete(@PathVariable @ApiParam(required = true) String id) {
        return personService.deleteById(id);
    }
}
