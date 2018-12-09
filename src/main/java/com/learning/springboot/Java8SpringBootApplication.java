package com.learning.springboot;

import com.learning.springboot.model.Authority;
import com.learning.springboot.model.Child;
import com.learning.springboot.model.Person;
import com.learning.springboot.repository.PersonRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootApplication
public class Java8SpringBootApplication extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(Java8SpringBootApplication.class);
	}

	public static void main(String[] args) {
		SpringApplication.run(Java8SpringBootApplication.class, args);
	}

	@Bean
	CommandLineRunner runner(PersonRepository personRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			if (personRepository.count() == 0) {
				Person person = new Person("Rodrigo Rodrigues", 35, "admin", passwordEncoder.encode("password"), permissions("ROLE_ADMIN"));
				person.setChildren(Arrays.asList(new Child("Daniel", 2), new Child("Oliver", 2)));
				personRepository.save(person);
				person = new Person("Juninho", 37, "master", passwordEncoder.encode("password123"), permissions("ROLE_CREATE", "ROLE_READ", "ROLE_SAVE"));
				person.setChildren(Arrays.asList(new Child("Dan", 5), new Child("Iam", 3)));
				personRepository.save(person);
				person = new Person("Anonymous", 30, "test", passwordEncoder.encode("test"), permissions("ROLE_CREATE"));
				personRepository.save(person);
			}

			personRepository.findAll()
					.stream()
					.forEach(System.out::println);
		};
	}

	private List<Authority> permissions(String ... permissions) {
		return Stream.of(permissions)
				.map(Authority::new)
				.collect(Collectors.toList());
	}
}
