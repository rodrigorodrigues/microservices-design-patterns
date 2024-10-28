package com.microservice.authentication.service;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Component;

@Slf4j
@AllArgsConstructor
@Component
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
    private final AuthenticationCommonRepository authenticationCommonRepository;
    private final SessionRepository sessionRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
        Authentication authentication) throws IOException, ServletException {
        if (response.isCommitted()) {
            return;
        }
        log.debug("CustomAuthenticationSuccessHandler:onAuthenticationSuccess:authentication: {}", authentication);
        DefaultOidcUser oidcUser = (DefaultOidcUser) authentication.getPrincipal();
        Map attributes = oidcUser.getAttributes();
        Optional<com.microservice.authentication.common.model.Authentication> findByEmail = authenticationCommonRepository.findByEmail(oidcUser.getEmail());
        Session session = sessionRepository.findById(request.getSession(false).getId());
        OAuth2AccessToken accessToken = session.getAttribute("token");
        response.addHeader(HttpHeaders.AUTHORIZATION, String.format("%s %s", accessToken.getTokenType().getValue(), accessToken.getTokenValue()));

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
