package com.microservice.user;

import com.microservice.authentication.autoconfigure.AuthenticationProperties;
import com.microservice.authentication.common.service.Base64DecodeUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.rsa.crypto.KeyStoreKeyFactory;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.security.interfaces.RSAPublicKey;

@SpringBootApplication
@EnableDiscoveryClient
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

    @Profile("kubernetes")
    @ConditionalOnMissingBean
    @Bean
    RSAPublicKey keyPair(AuthenticationProperties properties) {
        ResourceServerProperties.Jwt jwt = properties.getJwt();
        char[] password = Base64DecodeUtil.decodePassword(jwt.getKeyStorePassword());
        KeyStoreKeyFactory keyStoreKeyFactory = new KeyStoreKeyFactory(new FileSystemResource(jwt.getKeyStore().replaceFirst("file:", "")), password);
        return (RSAPublicKey) keyStoreKeyFactory.getKeyPair(jwt.getKeyAlias()).getPublic();
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
