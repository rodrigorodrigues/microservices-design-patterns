package com.microservice.person;

import com.github.javafaker.Address;
import com.github.javafaker.Faker;
import com.microservice.person.dto.PersonDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.http.*;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.client.RestTemplate;

import java.time.ZoneId;
import java.util.stream.IntStream;

@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
public class PersonServiceApplication {
    Faker faker = new Faker();

    public static void main(String[] args) {
		SpringApplication.run(PersonServiceApplication.class, args);
	}

    @ConditionalOnProperty(prefix = "load.data", name = "people", havingValue = "true")
    @Bean
    CommandLineRunner runner() {
        return args -> {
            RestTemplate restTemplate = new RestTemplate();

            Address address = faker.address();
            IntStream.range(0, 40).forEach(i -> {
                PersonDto personDto = PersonDto.builder()
                    .fullName(faker.name().fullName())
                    .address(new PersonDto.Address(null, address.fullAddress(), address.city(), address.state(), address.country(), address.zipCode()))
                    .dateOfBirth(faker.date().birthday().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                    .build();
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJyb2RyaWdvcm9kcmlndWVzd2ViQGdtYWlsLmNvbSIsImF1dGgiOiJST0xFX1VTRVIsU0NPUEVfaHR0cHM6Ly93d3cuZ29vZ2xlYXBpcy5jb20vYXV0aC91c2VyaW5mby5lbWFpbCxTQ09QRV9odHRwczovL3d3dy5nb29nbGVhcGlzLmNvbS9hdXRoL3VzZXJpbmZvLnByb2ZpbGUsU0NPUEVfb3BlbmlkIiwidXNlcl9uYW1lIjoiMTE0MTMwOTE2NDc2MzA4MDM4OTk5IiwiaXNzIjoiaHR0cHM6Ly9zcGVuZGluZ2JldHRlci5jb20iLCJmdWxsTmFtZSI6IlJvZHJpZ28gUm9kcmlndWVzIiwidHlwZSI6ImFjY2VzcyIsImF1dGhvcml0aWVzIjpbIlNDT1BFX2h0dHBzOi8vd3d3Lmdvb2dsZWFwaXMuY29tL2F1dGgvdXNlcmluZm8uZW1haWwiLCJTQ09QRV9vcGVuaWQiLCJST0xFX1VTRVIiLCJTQ09QRV9odHRwczovL3d3dy5nb29nbGVhcGlzLmNvbS9hdXRoL3VzZXJpbmZvLnByb2ZpbGUiXSwiY2xpZW50X2lkIjoiMTE0MTMwOTE2NDc2MzA4MDM4OTk5IiwiYXVkIjoiaHR0cHM6Ly9zcGVuZGluZ2JldHRlci5jb20iLCJuYmYiOjE2MDg2ODE2NDAsInNjb3BlIjpbImh0dHBzOi8vd3d3Lmdvb2dsZWFwaXMuY29tL2F1dGgvdXNlcmluZm8ucHJvZmlsZSIsImh0dHBzOi8vd3d3Lmdvb2dsZWFwaXMuY29tL2F1dGgvdXNlcmluZm8uZW1haWwiLCJvcGVuaWQiXSwiaW1hZ2VVcmwiOiJodHRwczovL2xoMy5nb29nbGV1c2VyY29udGVudC5jb20vYS0vQU9oMTRHajdOMkctYzBFcFFxZmJMNUl2RUM1WG5PUGNhNVZyMHRTQjZvRS1IQT1zOTYtYyIsIm5hbWUiOiJyb2RyaWdvcm9kcmlndWVzd2ViQGdtYWlsLmNvbSIsImV4cCI6MTYwODY4MzQ0MCwiZnJlc2giOnRydWUsImlhdCI6MTYwODY4MTY0MCwianRpIjoiMThkZTNmYTAtZjg2NC00NjgyLTk4ZmMtZDg4OGNiNDAzODY3In0.5HfCj27YQvlzMOyIX_J33KlsrtAeeLdeqQK-FiKGxll748CzOU1_4jtNW2vK_TBdFdbbbKywiz67BU7fI3UvpK-QlayYB1n0SXEJRwU0E2ElCja0vRFVinFcByKMU224_IJ2NTa2VN3Rmv5_add1B8wfVXfkC1SAFqOIByiD9Jh0rf0xdEdH7VR9L81T_Tje84BWJ31iRra9il5pGLSvJWUTs9vaGY4OshkPw-6Epv7qMP3OHVD2GGj089Dp7wcC4rQYYOCqxrNqv7oo8uOj8iEtyKWJvJZaICmSFm7alqUtRTd9X2lZoyLtHl6yYOCEKs59zTo-A5NAktn12NH8Rg");
                ResponseEntity<String> responseEntity = restTemplate.exchange("https://spendingbetter.com/api/people", HttpMethod.POST, new HttpEntity<>(personDto, httpHeaders), String.class);
                log.info("responseEntity: {}", responseEntity);
            });
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
}
