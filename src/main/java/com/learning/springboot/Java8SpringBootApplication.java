package com.learning.springboot;

import com.learning.springboot.model.Child;
import com.learning.springboot.model.Person;
import com.learning.springboot.repository.PersonRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;

@SpringBootApplication
public class Java8SpringBootApplication {

	public static void main(String[] args) {
		SpringApplication.run(Java8SpringBootApplication.class, args);
	}

	@Bean
	CommandLineRunner runner(PersonRepository personRepository) {
		return args -> {
			personRepository.save(new Person("Rodrigo Rodrigues", 35, Arrays.asList(new Child("Daniel", 2), new Child("Oliver", 2))));
			personRepository.save(new Person("Juninho", 37, Arrays.asList(new Child("Dan", 5), new Child("Iam", 3))));
			personRepository.save(new Person("Anonymous", 30, null));

			personRepository.findAll()
					.stream()
					.forEach(System.out::println);
		};
	}

}
