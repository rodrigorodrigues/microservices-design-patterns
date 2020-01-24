package com.microservice.person;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.microservice.authentication.common.model.Authentication;
import com.microservice.authentication.common.model.Authority;
import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import com.microservice.person.config.SpringSecurityAuditorAware;
import com.microservice.person.dto.PersonDto;
import com.microservice.person.model.Address;
import com.microservice.person.model.Person;
import com.microservice.person.repository.PersonRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.web.reactive.function.BodyInserters.fromObject;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = PersonServiceApplication.class,
		properties = {"configuration.swagger=false",
            "debug=true",
            "logging.level.com.microservice=debug"})
@AutoConfigureWebTestClient(timeout = "1s")
@Import(PersonServiceApplicationIntegrationTest.MockAuthenticationMongoConfiguration.class)
public class PersonServiceApplicationIntegrationTest {

	@Autowired
	WebTestClient client;

	@Autowired
    ObjectMapper objectMapper;

	@Autowired
    DefaultTokenServices defaultTokenServices;

	@Autowired
    AuthenticationRepository authenticationRepository;

	@Autowired
    PasswordEncoder passwordEncoder;

	@Autowired
    PersonRepository personRepository;

	@Autowired
    SpringSecurityAuditorAware springSecurityAuditorAware;

	Person person;

	Map<String, List<GrantedAuthority>> users = new HashMap<>();

    {
        users.put("admin@gmail.com", Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        users.put("anonymous@gmail.com", Arrays.asList(new SimpleGrantedAuthority("ROLE_PERSON_READ")));
        users.put("master@gmail.com", Arrays.asList(new SimpleGrantedAuthority("ROLE_PERSON_CREATE"),
            new SimpleGrantedAuthority("ROLE_PERSON_READ"),
            new SimpleGrantedAuthority("ROLE_PERSON_SAVE")));
    }

    @TestConfiguration
    @EnableMongoAuditing
    @EnableMongoRepositories(basePackageClasses = AuthenticationRepository.class, considerNestedRepositories = true)
    static class MockAuthenticationMongoConfiguration {
    }

    interface AuthenticationRepository extends AuthenticationCommonRepository, CrudRepository<Authentication, String> {
    }

    @BeforeEach
    public void setup() {
        users.entrySet().stream()
            .map(e -> Authentication.builder().email(e.getKey())
                .password(passwordEncoder.encode("password123"))
                .authorities(e.getValue().stream().map(a -> new Authority(a.getAuthority())).collect(Collectors.toList()))
                .fullName("Master of something")
                .enabled(true)
                .build())
        .forEach(authenticationRepository::save);

        Authentication authentication = authenticationRepository.findByEmail("master@gmail.com");
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(authentication, null, authentication.getAuthorities());

        springSecurityAuditorAware.setCurrentAuthenticatedUser(usernamePasswordAuthenticationToken);

        personRepository.save(Person.builder()
            .fullName("Test Master")
            .address(new Address("123", "123", "123", "123", "123"))
            .dateOfBirth(LocalDate.now())
            .build())
            .subscribe(p -> {
                assertThat(p.getCreatedByUser()).isEqualTo("master@gmail.com");
                this.person = p;
            });
    }

    @AfterEach
    public void tearDown() {
        authenticationRepository.deleteAll();
        personRepository.delete(person).subscribe(p -> log.debug("Deleted person entity"));
    }

    @Test
	@DisplayName("Test - When Calling GET - /api/persons should return filter list of people and response 200 - OK")
	public void shouldReturnListOfPersonsWhenCallApi() {
		String authorizationHeader = authorizationHeader("master@gmail.com");

		client.get().uri("/api/persons")
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader)
				.exchange()
				.expectStatus().isOk()
                .expectBodyList(PersonDto.class).hasSize(1);

        client.get().uri("/api/persons")
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
            .exchange()
            .expectStatus().isOk()
            .returnResult(String.class)
            .getResponseBody()
            .subscribe(json -> {
                log.debug("output: {}", json);
                assertThat((String)JsonPath.read(json, "$.createdByUser")).isEqualTo("master@gmail.com");
                assertThat((String)JsonPath.read(json, "$.createdDate")).isNotEmpty();
            });
	}

    @Test
    @DisplayName("Test - When Calling GET - /api/persons should return list of people and response 200 - OK")
    public void shouldReturnListOfAllPersonsWhenCallApi() {
        String authorizationHeader = authorizationHeader("admin@gmail.com");

        client.get().uri("/api/persons")
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(PersonDto.class).hasSize(4);
    }

	@Test
    @DisplayName("Test - When Calling POST - /api/persons should create a new person and response 201 - Created")
	public void shouldInsertNewPersonWhenCallApi() throws Exception {
		String authorizationHeader = authorizationHeader("master@gmail.com");
		PersonDto person = createPerson();

		client.post().uri("/api/persons")
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.body(fromObject(convertToJson(person)))
				.exchange()
				.expectStatus().isCreated()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.expectHeader().value(HttpHeaders.LOCATION, containsString("/api/persons/"))
				.expectBody()
                    .jsonPath("$.id").isNotEmpty()
                    .jsonPath("$.createdByUser").isEqualTo("master@gmail.com")
                    .consumeWith(c -> setId(person, c));

		assertThat(person.getId()).isNotEmpty();

		client.delete().uri("/api/persons/{id}", person.getId())
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader("admin@gmail.com"))
            .exchange()
            .expectStatus().is2xxSuccessful();
	}

    private void setId(PersonDto person, EntityExchangeResult<byte[]> c) {
        try {
            person.setId(objectMapper.readValue(c.getResponseBody(), PersonDto.class).getId());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Test - When Calling POST - /api/persons without mandatory field should response 400 - Bad Request")
	public void shouldResponseBadRequestWhenCallApiWithoutValidRequest() throws JsonProcessingException {
		String authorizationHeader = authorizationHeader("admin@gmail.com");

		PersonDto person = createPerson();
		person.setFullName("");

		client.post().uri("/api/persons")
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.body(fromObject(convertToJson(person)))
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody().jsonPath("$.message").value(containsString("fullName: size must be between 5 and 200"));
	}

	@Test
    @DisplayName("Test - When Calling POST - /api/persons without valid authorization should response 403 - Forbidden")
	public void shouldResponseForbiddenWhenCallApiWithoutRightPermission() throws Exception {
		String authorizationHeader = authorizationHeader("anonymous@gmail.com");

		PersonDto person = createPerson();

		client.post().uri("/api/persons")
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.body(fromObject(convertToJson(person)))
				.exchange()
				.expectStatus().isForbidden();
	}

	private String authorizationHeader(String user) {
        if (users.containsKey(user)) {
            Authentication authentication = authenticationRepository.findByEmail(user);
            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(authentication, null, authentication.getAuthorities());

            OAuth2Request oAuth2Request = new OAuth2Request(null, usernamePasswordAuthenticationToken.getName(), usernamePasswordAuthenticationToken.getAuthorities(),
                true, Collections.singleton("read"), null, null, null, null);
            OAuth2AccessToken enhance = defaultTokenServices.createAccessToken(new OAuth2Authentication(oAuth2Request, usernamePasswordAuthenticationToken));

            return enhance.getTokenType() + " " + enhance.getValue();
        } else {
            return null;
        }
	}

	private PersonDto createPerson() {
		return PersonDto.builder().fullName("Rodrigo")
			.dateOfBirth(LocalDate.of(1988, 1, 1))
			.children(Arrays.asList(new PersonDto.ChildrenDto("Daniel", LocalDate.of(2017, Month.JANUARY, 1)), new PersonDto.ChildrenDto("Oliver", LocalDate.of(2017, Month.JANUARY, 1))))
			.address(new PersonDto.Address(null, "50 Main Street", "Bray", "Co. Wicklow", "Ireland", "058 65412"))
			.build();
	}

	private String convertToJson(PersonDto person) throws JsonProcessingException {
        return objectMapper.writeValueAsString(person);
	}

}
