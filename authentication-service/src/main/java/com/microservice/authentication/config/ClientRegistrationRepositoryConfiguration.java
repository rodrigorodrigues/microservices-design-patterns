package com.microservice.authentication.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.oauth2.client.autoconfigure.ConditionalOnOAuth2ClientRegistrationProperties;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientPropertiesMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@Configuration(
    proxyBeanMethods = false
)
@ConditionalOnOAuth2ClientRegistrationProperties
@EnableConfigurationProperties({OAuth2ClientProperties.class})
@ConditionalOnMissingBean({ClientRegistrationRepository.class})
public class ClientRegistrationRepositoryConfiguration {
    @Bean
    InMemoryClientRegistrationRepository clientRegistrationRepository(OAuth2ClientProperties properties) {
        List<ClientRegistration> registrations = new ArrayList<>((new OAuth2ClientPropertiesMapper(properties)).asClientRegistrations().values());
        return new InMemoryClientRegistrationRepository(registrations);
    }
}
