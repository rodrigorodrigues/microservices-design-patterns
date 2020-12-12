package com.microservice.authentication.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;

@Slf4j
@Service
@AllArgsConstructor
public class RedisTokenStoreService {
    private final DefaultTokenServices defaultTokenServices;

    private final JwtAccessTokenConverter jwtAccessTokenConverter;

    private final RedisTokenStore redisTokenStore;

    public OAuth2AccessToken generateToken(Authentication authentication, OAuth2Authentication oAuth2Authentication) {
        Collection<OAuth2AccessToken> tokensByClientId = redisTokenStore.findTokensByClientId(authentication.getName());
        OAuth2AccessToken token;
        if (CollectionUtils.isEmpty(tokensByClientId)) {
            OAuth2AccessToken accessToken = defaultTokenServices.createAccessToken(oAuth2Authentication);
            token = jwtAccessTokenConverter.enhance(accessToken, oAuth2Authentication);
            redisTokenStore.storeAccessToken(token, oAuth2Authentication);
            log.debug("Created new token for: {}", authentication.getName());
            return token;
        } else {
            tokensByClientId.forEach(t -> log.debug("Token Found: {}", ToStringBuilder.reflectionToString(t)));
            return tokensByClientId.stream()
                .filter(t -> t.getAdditionalInformation() != null && t.getAdditionalInformation().containsKey("jti"))
                .findFirst()
                .orElseGet(() -> new ArrayList<>(tokensByClientId).get(0));
        }
    }

    public void removeAllTokensByAuthenticationUser(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            redisTokenStore.findTokensByClientId(authentication.getName())
                .forEach(redisTokenStore::removeAccessToken);
        }
    }
}
