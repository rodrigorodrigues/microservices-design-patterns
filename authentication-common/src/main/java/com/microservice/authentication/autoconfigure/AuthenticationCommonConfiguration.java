package com.microservice.authentication.autoconfigure;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.microservice.authentication.common.model.Authentication;
import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import com.microservice.authentication.common.service.Base64DecodeUtil;
import com.microservice.authentication.common.service.SharedAuthenticationServiceImpl;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.encrypt.KeyStoreKeyFactory;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthenticationProperties.class)
@EnableMongoAuditing
@EnableMongoRepositories(basePackageClasses = AuthenticationCommonRepository.class)
public class AuthenticationCommonConfiguration implements ApplicationContextAware {

    //private final List<JwtAccessTokenConverterConfigurer> configurers;

    private final AuthenticationProperties authenticationProperties;

    private ApplicationContext applicationContext;

    public AuthenticationCommonConfiguration(//ObjectProvider<List<JwtAccessTokenConverterConfigurer>> configurers,
        AuthenticationProperties authenticationProperties) {
        //this.configurers = configurers.getIfAvailable();
        this.authenticationProperties = authenticationProperties;
    }

    /*@Profile("auth")
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

    @Profile("auth")
    @Primary
    @Bean
    @ConditionalOnMissingBean(TokenStore.class)
    public TokenStore jwtTokenStore(JwtAccessTokenConverter jwtAccessTokenConverter) {
        return new JwtTokenStore(jwtAccessTokenConverter);
    }*/

    @Profile("auth")
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
        return (context) -> {
            log.info("jwtTokenCustomizer:tokenType: {}", context.getTokenType());
            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                context.getClaims().claims((claims) -> {

                    if (context.getPrincipal() instanceof Authentication auth) {
                        claims.put("name", auth.getFullName());
                        claims.put("sub", auth.getId());
                        claims.put("fullName", auth.getFullName());
                        Set<String> roles = auth.getAuthorities()
                            .stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
                        claims.put("authorities", roles);
                    } else if (context.getPrincipal() instanceof OidcUser oidcUser) {
                        claims.put("name", oidcUser.getEmail());
                        claims.put("sub", oidcUser.getEmail());
                        claims.put("fullName", oidcUser.getFullName());
                        claims.put("imageUrl", oidcUser.getPicture());

                        Set<String> roles = AuthorityUtils.authorityListToSet(oidcUser.getAuthorities())
                            .stream()
                            .map(c -> c.replaceFirst("^ROLE_", ""))
                            .collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
                        claims.put("authorities", roles);
                    } else if (context.getPrincipal() instanceof Jwt jwt) {
                        claims.put("sub", jwt.getSubject());
                        claims.put("name", jwt.getClaimAsString("name"));
                        claims.put("fullName", jwt.getClaimAsString("fullName"));
                    }
/*
                    claims.put("auth", context.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(joining(",")));
*/
                    claims.put("type", "access");
                    claims.put("fresh", true);
                    long currentTime = new Date().getTime() / 1000;
                    claims.put("iat", currentTime);
                    claims.put("nbf", currentTime);
                    claims.put("iss", authenticationProperties.getIssuer());
                    claims.put("aud", authenticationProperties.getAud());
                    claims.put("jti", UUID.randomUUID().toString());
                    //claims.put(JwtAccessTokenConverter.ACCESS_TOKEN_ID, GrantType.REFRESH_TOKEN.getValue());

                    log.info("jwtTokenCustomizer:claims: {}", claims);
                });
            }
        };
    }

    /*@Profile("auth")
    @Primary
    @Bean
    public JwtAccessTokenConverter jwtAccessTokenConverter() throws Exception {
        JwtAccessTokenConverter converter = new JwtAccessTokenConverter() {
            @Override
            public OAuth2AccessToken enhance(
                    OAuth2AccessToken accessToken,
                    OAuth2Authentication authentication) {

                OAuth2AccessToken enhance = super.enhance(accessToken, authentication);
                Map<String, Object> additionalInfo = new HashMap<>();
                if (authentication.getPrincipal() instanceof Authentication auth) {
                    additionalInfo.put("name", authentication.getName());
                    additionalInfo.put("sub", authentication.getName());
                    additionalInfo.put("fullName", auth.getFullName());
                } else if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
                    additionalInfo.put("name", oidcUser.getEmail());
                    additionalInfo.put("sub", oidcUser.getEmail());
                    additionalInfo.put("fullName", oidcUser.getFullName());
                    additionalInfo.put("imageUrl", oidcUser.getPicture());
                } else if (authentication.getPrincipal() instanceof Jwt jwt) {
                    additionalInfo.put("sub", jwt.getSubject());
                    additionalInfo.put("name", jwt.getClaimAsString("name"));
                    additionalInfo.put("fullName", jwt.getClaimAsString("fullName"));
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
                additionalInfo.put(JwtAccessTokenConverter.ACCESS_TOKEN_ID, GrantType.REFRESH_TOKEN.getValue());

                DefaultOAuth2AccessToken defaultOAuth2AccessToken = new DefaultOAuth2AccessToken(enhance);
                defaultOAuth2AccessToken.getAdditionalInformation().putAll(additionalInfo);
                defaultOAuth2AccessToken.setValue(encode(defaultOAuth2AccessToken, authentication));
                return defaultOAuth2AccessToken;
            }
        };
        AuthenticationProperties.Jwt jwt = this.authenticationProperties.getJwt();
        String keyValue = jwt. getKeyValue();
        if (StringUtils.isNotBlank(keyValue)) {
            if (!keyValue.startsWith("-----BEGIN")) {
                converter.setSigningKey(keyValue);
            }
            converter.setVerifierKey(keyValue);
        } else {
            try {
                KeyPair keyPair = applicationContext.getBean(KeyPair.class);
                converter.setKeyPair(keyPair);
            } catch (Exception ignored) {
                KeyPair keyPair = getKeyPair(authenticationProperties);
                converter.setKeyPair(keyPair);
            }
        }
        if (!CollectionUtils.isEmpty(this.configurers)) {
            AnnotationAwareOrderComparator.sort(this.configurers);
            for (JwtAccessTokenConverterConfigurer configurer : this.configurers) {
                configurer.configure(converter);
            }
        }
        return converter;
    }*/

    @Profile({"!dev & auth"})
    @ConditionalOnMissingBean
    @Bean
    KeyPair getKeyPair(AuthenticationProperties authenticationProperties) throws Exception {
        AuthenticationProperties.Jwt jwt = authenticationProperties.getJwt();
        Resource privateKeyStore = new FileSystemResource(jwt.getKeyStore().replaceFirst("file:", ""));
        String publicKeyStore = jwt.getPublicKeyStore();
        if (privateKeyStore.getFilename().endsWith(".pem") && publicKeyStore != null) {
            String privateKeyContent = new String(Files.readAllBytes(privateKeyStore.getFile().toPath()));
            byte[] encodedBytes = Base64.getDecoder().decode(removeBeginEnd(privateKeyContent));

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = kf.generatePrivate(keySpec);
            PublicKey publicKey = readPublicKey(new FileSystemResource(publicKeyStore.replaceFirst("file:", "")).getFile());
            return new KeyPair(publicKey, privateKey);
        } else {
            char[] keyStorePassword = Base64DecodeUtil.decodePassword(jwt.getKeyStorePassword());
            KeyStoreKeyFactory keyStoreKeyFactory = new KeyStoreKeyFactory(privateKeyStore, keyStorePassword);

            String keyAlias = jwt.getKeyAlias();
            return keyStoreKeyFactory.getKeyPair(keyAlias, keyStorePassword);
        }
    }

    private PublicKey readPublicKey(File publicKeyFile) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        byte[] encodedBytes;
        String publicKeyContent = new String(Files.readAllBytes(publicKeyFile.toPath()));
        encodedBytes = Base64.getDecoder().decode(removeBeginEnd(publicKeyContent));
        X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(encodedBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(pubSpec);
    }

    private String removeBeginEnd(String pem) {
        pem = pem.replaceAll("-----BEGIN (.*)-----", "");
        pem = pem.replaceAll("-----END (.*)----", "");
        pem = pem.replaceAll("\r\n", "");
        pem = pem.replaceAll("\n", "");
        return pem.trim();
    }

    @Profile("prod")
    @ConditionalOnMissingBean
    @Bean
    RSAPublicKey publicKey(KeyPair keyPair) {
        return (RSAPublicKey) keyPair.getPublic();
    }

    @Bean
    UserDetailsService sharedAuthenticationService(AuthenticationCommonRepository authenticationCommonRepository) {
        return new SharedAuthenticationServiceImpl(authenticationCommonRepository);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
