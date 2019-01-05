package com.learning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.springboot.Java8SpringBootApplication;
import com.learning.springboot.dto.LoginDto;
import com.learning.springboot.dto.PersonDto;
import com.learning.springboot.mapper.PersonMapper;
import com.learning.springboot.model.Address;
import com.learning.springboot.model.Authority;
import com.learning.springboot.model.Child;
import com.learning.springboot.model.Person;
import com.learning.springboot.repository.PersonRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.web.reactive.function.BodyInserters.fromObject;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Java8SpringBootApplication.class,
		properties = {"configuration.swagger=false",
				"debug=true",
				"logging.level.org.springframework=debug",
				"logging.level.com.learning=debug"})
public class Java8SpringBootApplicationIntegrationTest {

	@Autowired
	ApplicationContext context;

	@Autowired
	PersonMapper personMapper;

	@Autowired
	PersonRepository personRepository;

	WebTestClient client;

	@BeforeEach
	public void setup() {
        client = WebTestClient.bindToApplicationContext(context)
                .configureClient()
                .build();
	}

	@Test
	public void shouldReturnListOfPersonsWhenCallApi() {
		String authorizationHeader = authenticate("master", "password123");

		client.get().uri("/api/persons")
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader)
				.exchange()
				.expectStatus().isOk()
				.expectBodyList(PersonDto.class);
	}

	@Test
	public void shouldInsertNewPersonWhenCallApi() throws Exception {
		String authorizationHeader = authenticate("master", "password123");
		Person person = createPerson();

		client.post().uri("/api/persons")
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.body(fromObject(convertToJson(person)))
				.exchange()
				.expectStatus().isCreated()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.expectHeader().value(HttpHeaders.LOCATION, containsString("/api/persons/"))
				.expectBody().jsonPath("$.id").isNotEmpty();
	}

	@Test
	public void shouldResponseBadRequestWhenCallApiWithoutValidRequest() throws JsonProcessingException {
		String authorizationHeader = authenticate("admin", "password");

		Person person = createPerson();
		person.setName("");

		client.post().uri("/api/persons")
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.body(fromObject(convertToJson(person)))
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody().jsonPath("$.message").value(containsString("name: size must be between 5 and 100"));
	}

	@Test
	public void shouldResponseForbiddenWhenCallApiWithoutRightPermission() throws Exception {
		String authorizationHeader = authenticate("test", "test");

		Person person = createPerson();

		client.post().uri("/api/persons")
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.body(fromObject(convertToJson(person)))
				.exchange()
				.expectStatus().isForbidden();
	}

	private String authenticate(String user, String password) {
		FluxExchangeResult<Map> mapFluxExchangeResult = client.post().uri("/api/authenticate")
				.body(fromObject(new LoginDto(user, password, false)))
				.exchange()
				.expectStatus().is2xxSuccessful()
				.returnResult(Map.class);

		return (String) mapFluxExchangeResult.getResponseBody().blockLast().get("id_token");
	}

	private Person createPerson() {
		Person person = new Person("Rodrigo", 23, "rod@gmail.com", "rod", "123", Arrays.asList(new Authority("ROLE_USER")));
		person.setChildren(Arrays.asList(new Child("Daniel", 2), new Child("Oliver", 2)));
		person.setAddress(new Address("50 Main Street", "Bray", "Co. Wicklow", "Ireland", "058 65412"));
		return person;
	}

	private String convertToJson(Person person) throws JsonProcessingException {
		return new ObjectMapper().writeValueAsString(personMapper.map(person));
	}

	@AfterEach
	public void tearDown() {
		personRepository.findByUsername("rod")
				.map(personRepository::delete)
				.map(Mono::block);
	}
}
