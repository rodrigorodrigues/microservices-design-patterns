package com.learning.springboot.controller;

import com.learning.springboot.model.Person;
import com.learning.springboot.service.PersonService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/", "/persons"})
@AllArgsConstructor
public
class PersonController {
    private final PersonService personService;

    @GetMapping("/persons")
    public ResponseEntity<List<Person>> findAll() {
        return ResponseEntity.ok(personService.findAll());
    }
}
