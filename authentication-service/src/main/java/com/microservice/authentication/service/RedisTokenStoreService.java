package com.microservice.authentication.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.TokenRequest;

public interface RedisTokenStoreService {
    OAuth2AccessToken generateToken(OAuth2Authentication oAuth2Authentication);

    void removeAllTokensByAuthenticationUser(Authentication authentication);

    OAuth2AccessToken refreshToken(TokenRequest tokenRequest);

    OAuth2AccessToken getToken(Authentication authentication);
}
