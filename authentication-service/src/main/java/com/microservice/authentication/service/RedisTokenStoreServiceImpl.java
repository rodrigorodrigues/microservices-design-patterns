package com.microservice.authentication.service;

import java.util.Collections;
import java.util.Comparator;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;

@Slf4j
@AllArgsConstructor
public class RedisTokenStoreServiceImpl implements RedisTokenStoreService {
    private final DefaultTokenServices defaultTokenServices;

    private final RedisTokenStore redisTokenStore;

    @Override
    public OAuth2AccessToken generateToken(OAuth2Authentication oAuth2Authentication) {
        OAuth2AccessToken accessToken = defaultTokenServices.createAccessToken(oAuth2Authentication);
        log.debug("Created new token for: {}", oAuth2Authentication.getName());
        return accessToken;
    }

    @Override
    public void removeAllTokensByAuthenticationUser(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            redisTokenStore.findTokensByClientId(authentication.getName())
                .forEach(redisTokenStore::removeAccessToken);
        }
    }

    @Override
    public OAuth2AccessToken refreshToken(TokenRequest tokenRequest) {
        return defaultTokenServices.refreshAccessToken(tokenRequest.getRequestParameters().get("refresh_token"), tokenRequest);
    }

    @Override
    public OAuth2AccessToken getToken(Authentication authentication) {
        return redisTokenStore.findTokensByClientId(authentication.getName())
            .stream()
            .sorted(Comparator.comparing(OAuth2AccessToken::getExpiration).reversed())
            .filter(t -> t.getAdditionalInformation() != null && t.getAdditionalInformation().containsKey("jti"))
            .findFirst()
            .orElseGet(() -> {
                OAuth2Request oAuth2Request = new OAuth2Request(null, authentication.getName(), authentication.getAuthorities(),
                    true, Collections.singleton("read"), null, null, null, null);
                OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(oAuth2Request, authentication);
                return generateToken(oAuth2Authentication);
            });
    }
}
