package com.microservice.authentication.service;

import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Slf4j
@AllArgsConstructor
@Component
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
    private final AuthenticationCommonRepository authenticationCommonRepository;

    private final Oauth2TokenStoreService oauth2TokenStoreService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (response.isCommitted()) {
            return;
        }
        log.debug("CustomAuthenticationSuccessHandler:onAuthenticationSuccess:authentication: {}", authentication);
        DefaultOidcUser oidcUser = (DefaultOidcUser) authentication.getPrincipal();
        Map attributes = oidcUser.getAttributes();
        Optional<com.microservice.authentication.common.model.Authentication> findByEmail = authenticationCommonRepository.findByEmail(oidcUser.getEmail());
        OAuth2Request oAuth2Request = new OAuth2Request(attributes, authentication.getName(), authentication.getAuthorities(),
            true, (findByEmail.isPresent() ? findByEmail.get().getScopes() : null), null, null, null, null);
        OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(oAuth2Request, authentication);

        OAuth2AccessToken token = oauth2TokenStoreService.generateToken(oAuth2Authentication, true);
        response.addHeader(HttpHeaders.AUTHORIZATION, String.format("%s %s", token.getTokenType(), token.getValue()));

        super.onAuthenticationSuccess(request, response, authentication);
    }

}
