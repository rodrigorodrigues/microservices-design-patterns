package com.learning.springboot;

import com.learning.springboot.config.Java8SpringConfigurationProperties;
import com.learning.springboot.model.Address;
import com.learning.springboot.model.Authority;
import com.learning.springboot.model.Child;
import com.learning.springboot.model.Person;
import com.learning.springboot.repository.PersonRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@EnableConfigurationProperties(Java8SpringConfigurationProperties.class)
@SpringBootApplication
@EnableReactiveMongoRepositories(basePackageClasses = PersonRepository.class)
public class Java8SpringBootApplication {

	public static void main(String[] args) {
		SpringApplication.run(Java8SpringBootApplication.class, args);
	}

	@ConditionalOnProperty(prefix = "configuration", name = "initialLoad", havingValue = "true", matchIfMissing = true)
	@Bean
	CommandLineRunner runner(PersonRepository personRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			if (personRepository.count().block() == 0) {
				Person person = new Person("Rodrigo Rodrigues", 35, "admin@gmail.com", "admin", passwordEncoder.encode("password"), permissions("ROLE_ADMIN"));
				person.setChildren(Arrays.asList(new Child("Daniel", 2), new Child("Oliver", 2)));
				person.setAddress(new Address("50 Main Street", "Bray", "Co. Wicklow", "Ireland", "058 65412"));
				personRepository.save(person).block();

				person = new Person("Juninho", 37, "master@gmail.com", "master", passwordEncoder.encode("password123"), permissions("ROLE_CREATE", "ROLE_READ", "ROLE_SAVE"));
				person.setChildren(Arrays.asList(new Child("Dan", 5), new Child("Ian", 3)));
				person.setAddress(new Address("100 Gardiner Street", "Dun Laoghaire", "Dublin", "Ireland", "000 65412"));
				personRepository.save(person).block();

				person = new Person("Anonymous", 30, "anonymous@gmail.com", "test", passwordEncoder.encode("test"), permissions("ROLE_READ"));
				person.setAddress(new Address("10 Parnell Street", "Dublin 1", "Dublin", "Ireland", "111 65412"));
				personRepository.save(person).block();
			}

			personRepository.findAll()
					.doOnNext(System.out::println)
					.blockLast();
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
