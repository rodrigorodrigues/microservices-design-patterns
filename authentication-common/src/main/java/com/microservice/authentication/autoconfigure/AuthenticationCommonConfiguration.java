package com.microservice.authentication.autoconfigure;

import com.microservice.authentication.common.model.Authentication;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.oauth2.resource.JwtAccessTokenConverterConfigurer;
import org.springframework.boot.autoconfigure.security.oauth2.resource.JwtAccessTokenConverterRestTemplateCustomizer;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.joining;

@Configuration
public class AuthenticationCommonConfiguration {

    private final ResourceServerProperties resource;

    private final List<JwtAccessTokenConverterConfigurer> configurers;

    private final List<JwtAccessTokenConverterRestTemplateCustomizer> customizers;

    public AuthenticationCommonConfiguration(ResourceServerProperties resource,
                                         ObjectProvider<List<JwtAccessTokenConverterConfigurer>> configurers,
                                         ObjectProvider<List<JwtAccessTokenConverterRestTemplateCustomizer>> customizers) {
        this.resource = resource;
        this.configurers = configurers.getIfAvailable();
        this.customizers = customizers.getIfAvailable();
    }

    @Bean
    @ConditionalOnMissingBean(ResourceServerTokenServices.class)
    public DefaultTokenServices jwtTokenServices(TokenStore jwtTokenStore, JwtAccessTokenConverter jwtTokenEnhancer) {
        DefaultTokenServices services = new DefaultTokenServices();
        services.setTokenStore(jwtTokenStore);
        services.setTokenEnhancer(jwtTokenEnhancer);
        services.setSupportRefreshToken(true);
        return services;
    }

    @Bean
    @ConditionalOnMissingBean(TokenStore.class)
    public TokenStore jwtTokenStore() {
        return new JwtTokenStore(jwtTokenEnhancer());
    }

    @Bean
    public JwtAccessTokenConverter jwtTokenEnhancer() {
        JwtAccessTokenConverter converter = new JwtAccessTokenConverter() {
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
        String keyValue = this.resource.getJwt().getKeyValue();
        if (!org.springframework.util.StringUtils.hasText(keyValue)) {
            keyValue = getKeyFromServer();
        }
        if (org.springframework.util.StringUtils.hasText(keyValue) && !keyValue.startsWith("-----BEGIN")) {
            converter.setSigningKey(keyValue);
        }
        if (keyValue != null) {
            converter.setVerifierKey(keyValue);
        }
        if (!CollectionUtils.isEmpty(this.configurers)) {
            AnnotationAwareOrderComparator.sort(this.configurers);
            for (JwtAccessTokenConverterConfigurer configurer : this.configurers) {
                configurer.configure(converter);
            }
        }
        return converter;
    }

    private String getKeyFromServer() {
        RestTemplate keyUriRestTemplate = new RestTemplate();
        if (!CollectionUtils.isEmpty(this.customizers)) {
            for (JwtAccessTokenConverterRestTemplateCustomizer customizer : this.customizers) {
                customizer.customize(keyUriRestTemplate);
            }
        }
        HttpHeaders headers = new HttpHeaders();
        String username = this.resource.getClientId();
        String password = this.resource.getClientSecret();
        if (username != null && password != null) {
            byte[] token = Base64.getEncoder()
                .encode((username + ":" + password).getBytes());
            headers.add("Authorization", "Basic " + new String(token));
        }
        HttpEntity<Void> request = new HttpEntity<>(headers);
        String url = this.resource.getJwt().getKeyUri();
        return (String) keyUriRestTemplate
            .exchange(url, HttpMethod.GET, request, Map.class).getBody()
            .get("value");
    }

}
