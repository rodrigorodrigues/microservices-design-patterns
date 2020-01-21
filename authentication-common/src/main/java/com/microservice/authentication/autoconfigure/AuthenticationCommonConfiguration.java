package com.microservice.authentication.autoconfigure;

import com.microservice.authentication.common.model.Authentication;
import com.microservice.jwt.common.config.Java8SpringConfigurationProperties;
import lombok.AllArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;

import java.util.HashMap;
import java.util.Map;

import static java.util.stream.Collectors.joining;

@Configuration
@AllArgsConstructor
public class AuthenticationCommonConfiguration {
    private final Java8SpringConfigurationProperties configurationProperties;

    @Bean
    public TokenStore tokenStore() {
        return new JwtTokenStore(jwtAccessTokenConverter());
    }

    @Bean
    JwtAccessTokenConverter jwtAccessTokenConverter() {
        JwtAccessTokenConverter jwtAccessTokenConverter = new JwtAccessTokenConverter() {
            @Override
            public OAuth2AccessToken enhance(
                OAuth2AccessToken accessToken,
                OAuth2Authentication authentication) {
                Map<String, Object> additionalInfo = new HashMap<>();
                if (authentication.getUserAuthentication() instanceof Authentication) {
                    additionalInfo.put("name",
                        ((Authentication) authentication.getUserAuthentication().getPrincipal()).getFullName());
                }
                additionalInfo.put("auth", authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(joining(",")));
                additionalInfo.put("type", "access");
                additionalInfo.put("fresh", true);
                ((DefaultOAuth2AccessToken) accessToken).setAdditionalInformation(additionalInfo);
                return super.enhance(accessToken, authentication);
            }
        };
        Java8SpringConfigurationProperties.Jwt jwt = configurationProperties.getJwt();
        if (StringUtils.isNotBlank(jwt.getBase64Secret())) {
            jwtAccessTokenConverter.setSigningKey(jwt.getBase64Secret());
            jwtAccessTokenConverter.setVerifierKey(jwt.getBase64Secret());
        } else {
            KeyStoreKeyFactory keyStoreKeyFactory = new KeyStoreKeyFactory(new FileSystemResource(jwt.getKeystore()), jwt.getKeystorePassword().toCharArray());
            jwtAccessTokenConverter.setKeyPair(keyStoreKeyFactory.getKeyPair(jwt.getKeystoreAlias()));
        }
        return jwtAccessTokenConverter;
    }

}
