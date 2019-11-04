package com.microservice.user;

import com.microservice.authentication.common.model.Authority;
import com.microservice.authentication.common.service.ReactivePreAuthenticatedAuthenticationManager;
import com.microservice.authentication.common.service.SharedAuthenticationService;
import com.microservice.user.model.User;
import com.microservice.user.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.net.ssl.HttpsURLConnection;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
public class UserServiceApplication {
    static {
        HttpsURLConnection.setDefaultHostnameVerifier(new NoopHostnameVerifier());
    }

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

    @ConditionalOnProperty(prefix = "configuration", name = "initialLoad", havingValue = "true", matchIfMissing = true)
    @Bean
    CommandLineRunner runner(PasswordEncoder passwordEncoder, UserRepository userRepository) {
        return args -> {
            log.debug("Creating default users");
            userRepository.findByEmail("admin@gmail.com")
                    .switchIfEmpty(userRepository.save(User.builder().email("admin@gmail.com")
                    .password(passwordEncoder.encode("password"))
                    .authorities(permissions("ROLE_ADMIN"))
                    .fullName("Admin dos Santos")
                    .build()))
                    .subscribe(u -> log.debug("Created Admin User: {}", u));

            userRepository.findByEmail("anonymous@gmail.com")
                    .switchIfEmpty(userRepository.save(User.builder().email("anonymous@gmail.com")
                    .password(passwordEncoder.encode("test"))
                    .authorities(permissions("ROLE_PERSON_READ"))
                    .fullName("Anonymous Noname")
                    .build()))
                    .subscribe(u -> log.debug("Created Anonymous User: {}", u));

            userRepository.findByEmail("master@gmail.com")
                    .switchIfEmpty(userRepository.save(User.builder().email("master@gmail.com")
                    .password(passwordEncoder.encode("password123"))
                    .authorities(permissions("ROLE_PERSON_CREATE", "ROLE_PERSON_READ", "ROLE_PERSON_SAVE"))
                    .fullName("Master of something")
                    .build()))
                    .subscribe(u -> log.debug("Created Master User: {}", u));
        };
    }

    private List<Authority> permissions(String ... permissions) {
        return Stream.of(permissions)
                .map(Authority::new)
                .collect(Collectors.toList());
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

    @Bean
    ReactivePreAuthenticatedAuthenticationManager customReactiveAuthenticationManager(
        SharedAuthenticationService sharedAuthenticationService) {
        return new ReactivePreAuthenticatedAuthenticationManager(sharedAuthenticationService);
    }
}
