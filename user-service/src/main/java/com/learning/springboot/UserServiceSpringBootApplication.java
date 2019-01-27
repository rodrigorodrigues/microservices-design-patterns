package com.learning.springboot;

import com.learning.springboot.model.Authority;
import com.learning.springboot.model.User;
import com.learning.springboot.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
public class UserServiceSpringBootApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceSpringBootApplication.class, args);
    }

    @ConditionalOnProperty(prefix = "configuration", name = "initialLoad", havingValue = "true", matchIfMissing = true)
    @Bean
    CommandLineRunner runner(PasswordEncoder passwordEncoder, UserRepository userRepository) {
        return args -> {
            log.debug("Creating default users");
            userRepository.save(User.builder().email("admin@gmail.com")
                    .password(passwordEncoder.encode("password"))
                    .authorities(permissions("ROLE_ADMIN"))
                    .fullName("Admin dos Santos")
                    .build()).subscribe(u -> log.debug("Created Admin User: {}", u));

            userRepository.save(User.builder().email("anonymous@gmail.com")
                    .password(passwordEncoder.encode("test"))
                    .authorities(permissions("ROLE_READ"))
                    .fullName("Anonymous Noname")
                    .build()).subscribe(u -> log.debug("Created Anonymous User: {}", u));

            userRepository.save(User.builder().email("master@gmail.com")
                    .password(passwordEncoder.encode("password123"))
                    .authorities(permissions("ROLE_CREATE", "ROLE_READ", "ROLE_SAVE"))
                    .fullName("Master of something")
                    .build()).subscribe(u -> log.debug("Created Master User: {}", u));

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

    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

}
