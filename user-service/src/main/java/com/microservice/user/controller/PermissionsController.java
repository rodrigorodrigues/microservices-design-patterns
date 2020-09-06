package com.microservice.user.controller;

import com.microservice.web.common.util.constants.Permissions;
import lombok.AllArgsConstructor;
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
    public ResponseEntity<List<PermissionsDto>> getPermissions() {
        return ResponseEntity.ok(
                Stream.of(Permissions.values())
                .map(PermissionsDto::new)
                .collect(Collectors.toList()));
    }

    @AllArgsConstructor
    static class PermissionsDto {
        private final Permissions permissions;

        public String getType() {
            return permissions.getType();
        }

        public List<String> getPermissions() {
            return permissions.getPermissions();
        }
    }
}
