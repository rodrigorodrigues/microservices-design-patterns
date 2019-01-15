package com.learning.springboot;

import com.learning.springboot.config.Java8SpringConfigurationProperties;
import com.learning.springboot.model.*;
import com.learning.springboot.repository.PersonRepository;
import com.learning.springboot.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@EnableConfigurationProperties(Java8SpringConfigurationProperties.class)
@SpringBootApplication
public class Java8SpringBootApplication {

	public static void main(String[] args) {
		SpringApplication.run(Java8SpringBootApplication.class, args);
	}

	@ConditionalOnProperty(prefix = "configuration", name = "initialLoad", havingValue = "true", matchIfMissing = true)
	@Bean
	CommandLineRunner runner(PasswordEncoder passwordEncoder, UserRepository userRepository,
                             PersonRepository personRepository) {
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

			personRepository.save(Person.builder().fullName("Rodrigo Rodrigues")
					.dateOfBirth(LocalDate.of(1983, 1, 5))
					.children(Arrays.asList(new Child("Daniel", 2), new Child("Oliver", 2)))
					.address(new Address("50 Main Street", "Bray", "Co. Wicklow", "Ireland", "058 65412"))
					.build()).block();

			personRepository.save(Person.builder().fullName("Juninho")
					.dateOfBirth(LocalDate.of(1981, 5 , 25))
					.children(Arrays.asList(new Child("Dan", 5), new Child("Ian", 3)))
					.address(new Address("100 Gardiner Street", "Dun Laoghaire", "Dublin", "Ireland", "000 65412"))
					.build()).block();


			personRepository.save(Person.builder().fullName("Anonymous")
					.dateOfBirth(LocalDate.of(1985, 1, 2))
					.address(new Address("10 Parnell Street", "Dublin 1", "Dublin", "Ireland", "111 65412"))
					.build()).block();

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
