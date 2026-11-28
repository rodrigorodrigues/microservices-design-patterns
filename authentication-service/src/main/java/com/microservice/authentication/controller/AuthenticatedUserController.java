package com.microservice.authentication.controller;

import java.util.UUID;

import com.microservice.authentication.service.GenerateToken;
import com.microservice.authentication.service.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for authenticated user.
 */
@Slf4j
@RestController
public class AuthenticatedUserController {

    private final SessionRepository sessionRepository;

    private final GenerateToken generateToken;

    public AuthenticatedUserController(SessionRepository sessionRepository, GenerateToken generateToken) {
        this.sessionRepository = sessionRepository;
        this.generateToken = generateToken;
    }

    @GetMapping("/api/authenticatedUser")
    public ResponseEntity<TokenResponse> authenticatedUser(Authentication authentication, HttpServletRequest request) {
        log.info("Generating token for user: {}", authentication.getName());
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        HttpSession httpSession = request.getSession();
        Session session;
        OAuth2AccessToken accessToken = null;
        String refreshToken = null;
        if (httpSession != null) {
            session = sessionRepository.findById(httpSession.getId());
            httpHeaders.add("sessionId", httpSession.getId());
        } else {
            session = sessionRepository.findById(request.getHeader("sessionId"));
            httpHeaders.add("sessionId", request.getHeader("sessionId"));
        }
        if (session != null) {
            accessToken = session.getAttribute("token");
            refreshToken = session.getAttribute("refreshToken");
        }

        boolean sessionNeedsSave = false;
        if (accessToken == null) {
            accessToken = generateToken.generateToken(authentication);
            if (session != null) {
                session.setAttribute("token", accessToken);
                sessionNeedsSave = true;
            }
        }
        if (refreshToken == null) {
            refreshToken = UUID.randomUUID().toString();
            if (session != null) {
                session.setAttribute("refreshToken", refreshToken);
                sessionNeedsSave = true;
            }
        }
        if (sessionNeedsSave) {
            sessionRepository.save(session);
        }

        httpHeaders.add(HttpHeaders.AUTHORIZATION, String.format("%s %s", accessToken.getTokenType().getValue(), accessToken.getTokenValue()));
        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(httpHeaders)
            .body(TokenResponse.from(accessToken, refreshToken));
    }

    /**
     * Refreshes an access token using a previously issued refresh token, without
     * requiring a still-valid bearer token - that's the whole point (an expired
     * access token is exactly when the frontend calls this). The caller's identity
     * comes from the stored session's SecurityContext, not from SecurityContextHolder
     * (which won't have one - this endpoint is permitAll and this app's /api/**
     * filter chain authenticates via bearer JWT, not the session cookie).
     */
    @PostMapping(value = "/api/refreshToken", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<TokenResponse> refreshToken(
        @RequestParam("refresh_token") String refreshToken,
        @RequestParam(value = "sessionId", required = false) String sessionIdParam,
        HttpServletRequest request) {

        String sessionId = resolveSessionId(sessionIdParam, request);
        Session session = sessionId != null ? sessionRepository.findById(sessionId) : null;
        String storedRefreshToken = session != null ? session.getAttribute("refreshToken") : null;

        if (session == null || storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            log.warn("refreshToken: rejected for sessionId={} (session found: {}, refresh token matched: {})",
                sessionId, session != null, storedRefreshToken != null && storedRefreshToken.equals(refreshToken));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        SecurityContext securityContext = session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        Authentication authentication = securityContext != null ? securityContext.getAuthentication() : null;
        if (authentication == null) {
            log.warn("refreshToken: no SecurityContext stored in sessionId={}", sessionId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        OAuth2AccessToken newAccessToken = generateToken.generateToken(authentication);
        String newRefreshToken = UUID.randomUUID().toString();
        session.setAttribute("token", newAccessToken);
        session.setAttribute("refreshToken", newRefreshToken);
        sessionRepository.save(session);

        log.debug("refreshToken: issued new token for sessionId={}", sessionId);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.add(HttpHeaders.AUTHORIZATION, String.format("%s %s", newAccessToken.getTokenType().getValue(), newAccessToken.getTokenValue()));
        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(httpHeaders)
            .body(TokenResponse.from(newAccessToken, newRefreshToken));
    }

    private String resolveSessionId(String sessionIdParam, HttpServletRequest request) {
        if (sessionIdParam != null) {
            return sessionIdParam;
        }
        HttpSession httpSession = request.getSession(false);
        if (httpSession != null) {
            return httpSession.getId();
        }
        return request.getHeader("sessionId");
    }
}
