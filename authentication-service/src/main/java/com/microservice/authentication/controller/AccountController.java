package com.microservice.authentication.controller;

import java.util.Optional;

import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import com.microservice.authentication.service.GenerateToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/account")
@AllArgsConstructor
public class AccountController {
    private final SessionRepository sessionRepository;

    private final GenerateToken generateToken;

    private final AuthenticationCommonRepository authenticationCommonRepository;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public UserDto index(Authentication authentication, HttpServletRequest request) {
        String login;
        String fullName;
        String email;
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            login = oidcUser.getName();
            fullName = oidcUser.getFullName();
            email = oidcUser.getEmail();
        } else {
            Optional<com.microservice.authentication.common.model.Authentication> findById = authenticationCommonRepository.findByEmail(authentication.getName());
            if (findById.isPresent()) {
                com.microservice.authentication.common.model.Authentication authenticationDb = findById.get();
                login = authenticationDb.getUsername();
                fullName = authenticationDb.getFullName();
                email = authenticationDb.getEmail();
            } else {
                login = authentication.getName();
                fullName = login;
                email = login;
            }
        }
        HttpSession httpSession = request.getSession();
        Session session;
        OAuth2AccessToken accessToken = null;
        if (httpSession != null) {
            session = sessionRepository.findById(httpSession.getId());
            if (session != null) {
                accessToken = session.getAttribute("token");
            }
        } else {
            session = sessionRepository.findById(request.getHeader("sessionId"));
            if (session != null) {
                accessToken = session.getAttribute("token");
            }
        }
        if (accessToken == null) {
            accessToken = generateToken.generateToken(authentication);
            if (session != null) {
                session.setAttribute("token", accessToken);
            }
        }
        log.debug("accessToken: {}", accessToken);
        String[] authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
            .toList()
            .toArray(new String[] {});
        UserDto userDto = new UserDto(login, fullName, email, authorities);
        log.debug("AccountController:userDto: {}", userDto);
        return userDto;
    }

    public record UserDto(String login, String langKey, String fullName, String email, boolean activated, String[] authorities) {
        public UserDto(String login, String fullName, String email, String[] authorities) {
            this(login, "en", fullName, email, true, authorities);
        }
    }
}
