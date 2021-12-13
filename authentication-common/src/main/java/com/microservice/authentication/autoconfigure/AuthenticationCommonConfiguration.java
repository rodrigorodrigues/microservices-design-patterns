package com.microservice.authentication.autoconfigure;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.microservice.authentication.common.model.Authentication;
import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import com.microservice.authentication.common.service.Base64DecodeUtil;
import com.microservice.authentication.common.service.SharedAuthenticationServiceImpl;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.oauth2.resource.JwtAccessTokenConverterConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;
import org.springframework.util.CollectionUtils;

import static java.util.stream.Collectors.joining;

@Slf4j
@Configuration
@EnableConfigurationProperties(AuthenticationProperties.class)
@EnableMongoAuditing
@EnableMongoRepositories(basePackageClasses = AuthenticationCommonRepository.class)
public class AuthenticationCommonConfiguration {

    private final List<JwtAccessTokenConverterConfigurer> configurers;

    private final AuthenticationProperties authenticationProperties;

    public AuthenticationCommonConfiguration(ObjectProvider<List<JwtAccessTokenConverterConfigurer>> configurers, AuthenticationProperties authenticationProperties) {
        this.configurers = configurers.getIfAvailable();
        this.authenticationProperties = authenticationProperties;
    }

    @Primary
    @Bean
    @ConditionalOnMissingBean(ResourceServerTokenServices.class)
    public DefaultTokenServices jwtTokenServices(TokenStore jwtTokenStore, JwtAccessTokenConverter jwtTokenEnhancer) {
        DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
        defaultTokenServices.setTokenStore(jwtTokenStore);
        defaultTokenServices.setTokenEnhancer(jwtTokenEnhancer);
        defaultTokenServices.setSupportRefreshToken(true);
        defaultTokenServices.setAccessTokenValiditySeconds(60 * 30);
        return defaultTokenServices;
    }

    @Primary
    @Bean
    @ConditionalOnMissingBean(TokenStore.class)
    public TokenStore jwtTokenStore(JwtAccessTokenConverter jwtAccessTokenConverter) {
        return new JwtTokenStore(jwtAccessTokenConverter);
    }

    @Primary
    @Bean
    public JwtAccessTokenConverter jwtAccessTokenConverter() {
        JwtAccessTokenConverter converter = new JwtAccessTokenConverter() {
            @Override
            public OAuth2AccessToken enhance(
                OAuth2AccessToken accessToken,
                OAuth2Authentication authentication) {
                Map<String, Object> additionalInfo = new HashMap<>();
                if (authentication.getUserAuthentication() instanceof Authentication) {
                    additionalInfo.put("name",
                        ((Authentication) authentication.getUserAuthentication().getPrincipal()).getFullName());
                    additionalInfo.put("sub", authentication.getName());
                } else if (authentication.getPrincipal() instanceof OidcUser) {
                    DefaultOidcUser oidcUser = (DefaultOidcUser) authentication.getPrincipal();
                    additionalInfo.put("name", oidcUser.getEmail());
                    additionalInfo.put("sub", oidcUser.getEmail());
                    additionalInfo.put("fullName", oidcUser.getFullName());
                    additionalInfo.put("imageUrl", oidcUser.getPicture());
                } else {
                    additionalInfo.put("sub", authentication.getName());
                }
                additionalInfo.put("auth", authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(joining(",")));
                additionalInfo.put("type", "access");
                additionalInfo.put("fresh", true);
                long currentTime = new Date().getTime() / 1000;
                additionalInfo.put("iat", currentTime);
                additionalInfo.put("nbf", currentTime);
                additionalInfo.put("iss", authenticationProperties.getIssuer());
                additionalInfo.put("aud", authenticationProperties.getAud());
                additionalInfo.put("jti", UUID.randomUUID().toString());

                DefaultOAuth2AccessToken defaultOAuth2AccessToken = new DefaultOAuth2AccessToken(accessToken);
                defaultOAuth2AccessToken.setAdditionalInformation(additionalInfo);
                defaultOAuth2AccessToken.setValue(encode(defaultOAuth2AccessToken, authentication));
                return defaultOAuth2AccessToken;
            }
        };
        AuthenticationProperties.Jwt jwt = this.authenticationProperties.getJwt();
        String keyValue = jwt.getKeyValue();
        if (StringUtils.isNotBlank(keyValue)) {
            if (!keyValue.startsWith("-----BEGIN")) {
                converter.setSigningKey(keyValue);
            }
            converter.setVerifierKey(keyValue);
        } else if (jwt.getKeyStore() != null) {
            KeyPair keyPair = getKeyPair(authenticationProperties);
            converter.setKeyPair(keyPair);
        }
        if (!CollectionUtils.isEmpty(this.configurers)) {
            AnnotationAwareOrderComparator.sort(this.configurers);
            for (JwtAccessTokenConverterConfigurer configurer : this.configurers) {
                configurer.configure(converter);
            }
        }
        return converter;
    }

    @Profile("prod")
    @Bean
    KeyPair getKeyPair(AuthenticationProperties authenticationProperties) {
        AuthenticationProperties.Jwt jwt = authenticationProperties.getJwt();
        Resource keyStore = new FileSystemResource(jwt.getKeyStore().replaceFirst("file:", ""));
        char[] keyStorePassword = Base64DecodeUtil.decodePassword(jwt.getKeyStorePassword());
        KeyStoreKeyFactory keyStoreKeyFactory = new KeyStoreKeyFactory(keyStore, keyStorePassword);

        String keyAlias = jwt.getKeyAlias();
        return keyStoreKeyFactory.getKeyPair(keyAlias, keyStorePassword);
    }


    @Profile("prod")
    @Bean
    RSAPublicKey publicKey(KeyPair keyPair) {
        return (RSAPublicKey) keyPair.getPublic();
    }

    @Bean
    UserDetailsService sharedAuthenticationService(AuthenticationCommonRepository authenticationCommonRepository) {
        return new SharedAuthenticationServiceImpl(authenticationCommonRepository);
    }
}
