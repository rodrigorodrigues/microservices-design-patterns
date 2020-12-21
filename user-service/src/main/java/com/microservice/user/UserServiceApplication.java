package com.microservice.user;

import com.microservice.authentication.autoconfigure.AuthenticationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.server.Ssl;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.rsa.crypto.KeyStoreKeyFactory;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

    @Profile("!kubernetes & !test")
    @ConditionalOnMissingBean
    @Bean
    RSAPublicKey keyPair(AuthenticationProperties properties) {
        ResourceServerProperties.Jwt jwt = properties.getJwt();
        char[] password = getSslPassword(jwt.getKeyStorePassword());
        KeyStoreKeyFactory keyStoreKeyFactory = new KeyStoreKeyFactory(new FileSystemResource(jwt.getKeyStore().replaceFirst("file:", "")), password);
        return (RSAPublicKey) keyStoreKeyFactory.getKeyPair(jwt.getKeyAlias()).getPublic();
    }

    @Profile("kubernetes__")
    @ConditionalOnMissingBean
    @Bean
    RSAPublicKey keyPairSsl(@Value("${server.ssl.key-store}") Resource keystore, ServerProperties serverProperties) {
        Ssl ssl = serverProperties.getSsl();
        return (RSAPublicKey) new KeyStoreKeyFactory(keystore, ssl.getKeyStorePassword().toCharArray())
                .getKeyPair(ssl.getKeyAlias())
                .getPublic();
    }

    private char[] getSslPassword(String password) {
        try {
            return new String(Base64.getDecoder().decode(password)).toCharArray();
        } catch (Exception e) {
            return password.toCharArray();
        }
    }

    @Bean
    public ValidatingMongoEventListener validatingMongoEventListener() {
        return new ValidatingMongoEventListener(validator());
    }

    @Primary
    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
