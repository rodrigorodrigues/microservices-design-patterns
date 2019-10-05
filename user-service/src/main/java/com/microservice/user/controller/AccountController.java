package com.microservice.user.controller;

import com.microservice.user.dto.UserDto;
import com.microservice.user.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/account")
@AllArgsConstructor
public class AccountController {
    private final UserService userService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<UserDto> index(@AuthenticationPrincipal Authentication authentication) {
        return userService.findById(authentication.getName());
    }
}
