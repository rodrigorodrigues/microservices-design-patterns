package com.microservice.user.controller;

import com.microservice.web.common.util.constants.Permissions;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/users/permissions")
public class PermissionsController {
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> getPermissions() {
        return ResponseEntity.ok(Stream.of(Permissions.values())
                .map(Permissions::name)
                .collect(Collectors.toList()));
    }
}
