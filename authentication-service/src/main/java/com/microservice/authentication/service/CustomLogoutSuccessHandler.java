package com.microservice.authentication.service;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.session.SessionRepository;

public class CustomLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {
    private final SessionRepository sessionRepository;

    public CustomLogoutSuccessHandler(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        sessionRepository.deleteById(request.getSession(false).getId());
        super.onLogoutSuccess(request, response, authentication);
    }
}
