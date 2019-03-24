package com.learning.springboot;

import com.learning.springboot.model.Address;
import com.learning.springboot.model.Child;
import com.learning.springboot.model.Person;
import com.learning.springboot.repository.PersonRepository;
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

import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;

@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
public class PersonServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PersonServiceApplication.class, args);
	}

	@ConditionalOnProperty(prefix = "configuration", name = "initialLoad", havingValue = "true", matchIfMissing = true)
	@Bean
	CommandLineRunner runner(PersonRepository personRepository) {
		return args -> {
		    personRepository.count()
                .filter(c -> c == 0)
                .thenReturn(personRepository.saveAll(Arrays.asList(Person.builder().fullName("Rodrigo Rodrigues")
                    .dateOfBirth(LocalDate.of(1983, 1, 5))
                    .children(Arrays.asList(new Child("Daniel", LocalDate.of(2017, Month.JANUARY, 1)), new Child("Oliver", LocalDate.of(2017, Month.JANUARY, 1))))
                    .address(new Address("50 Main Street", "Bray", "Co. Wicklow", "Ireland", "058 65412"))
                    .build(),
                    Person.builder().fullName("Juninho")
                        .dateOfBirth(LocalDate.of(1981, 5, 25))
                        .children(Arrays.asList(new Child("Dan", LocalDate.of(2013, Month.JUNE, 10)), new Child("Ian", LocalDate.of(2016, Month.AUGUST, 18))))
                        .address(new Address("100 Gardiner Street", "Dun Laoghaire", "Dublin", "Ireland", "000 65412"))
                        .build(),
                    Person.builder().fullName("Anonymous")
                        .dateOfBirth(LocalDate.of(1985, 1, 2))
                        .address(new Address("10 Parnell Street", "Dublin 1", "Dublin", "Ireland", "111 65412"))
                        .build())).blockLast());
		};
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
