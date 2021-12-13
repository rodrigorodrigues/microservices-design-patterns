package com.microservice.person;

import java.time.ZoneId;
import java.util.Properties;
import java.util.stream.IntStream;

import com.github.javafaker.Address;
import com.github.javafaker.Faker;
import com.microservice.person.dto.PersonDto;
import com.microservice.person.repository.PersonRepository;
import com.microservice.person.service.PersonService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.info.GitProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Slf4j
@SpringBootApplication
public class PersonServiceApplication implements EnvironmentAware {
    private Environment env;
    Faker faker = new Faker();

    public static void main(String[] args) {
		SpringApplication.run(PersonServiceApplication.class, args);
	}

    @ConditionalOnProperty(prefix = "load.data", name = "people", havingValue = "true")
    @Bean
    CommandLineRunner runner(PersonService personService, @Value("${load.data.people.total:40}") Integer total, PersonRepository personRepository) {
        return args -> {
            if (personRepository.count() == 0) {
                Address address = faker.address();
                IntStream.range(0, total).forEach(i -> {
                    PersonDto personDto = PersonDto.builder()
                        .fullName(faker.name().fullName())
                        .address(new PersonDto.Address(null, address.fullAddress(), address.city(), address.state(), address.country(), address.zipCode()))
                        .dateOfBirth(faker.date().birthday().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                        .build();
                    log.info("personDto: {}", personService.save(personDto));
                });
            }
        };
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

    @ConditionalOnMissingBean
    @Bean
    GitProperties gitProperties() {
        return new GitProperties(new Properties());
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.env = environment;
    }
}
