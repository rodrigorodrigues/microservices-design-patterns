package com.learning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.springboot.Java8SpringBootApplication;
import com.learning.springboot.dto.LoginDto;
import com.learning.springboot.mapper.PersonMapper;
import com.learning.springboot.model.Address;
import com.learning.springboot.model.Authority;
import com.learning.springboot.model.Child;
import com.learning.springboot.model.Person;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

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

	WebTestClient client;

	private String authorizationHeader;

	@BeforeEach
	public void setup() {
        client = WebTestClient.bindToApplicationContext(context)
                .configureClient()
                .build();

		if (StringUtils.isBlank(authorizationHeader)) {
			FluxExchangeResult<Map> mapFluxExchangeResult = client.post().uri("/api/authenticate")
					.body(fromObject(new LoginDto("admin", "password", false)))
					.exchange()
					.expectStatus().is2xxSuccessful()
					.returnResult(Map.class);

			authorizationHeader = (String) mapFluxExchangeResult.getResponseBody().blockLast().get("id_token");
		}
	}

	@Test
	public void shouldInsertNewPersonWhenCallApi() throws Exception {
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
	@WithMockUser(roles = "CREATE")
	public void shouldResponseBadRequestWhenCallApiWithoutValidRequest() throws JsonProcessingException {
		Person person = createPerson();
		person.setName("");

		client.post().uri("/api/persons")
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.body(fromObject(convertToJson(person)))
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody().jsonPath("$.message").isNotEmpty();
	}

	@Test
	@WithMockUser(roles = "READ")
	public void shouldResponseForbiddenWhenCallApiWithoutRightPermission() throws Exception {
		Person person = createPerson();

		client.post().uri("/api/persons")
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.body(fromObject(convertToJson(person)))
				.exchange()
				.expectStatus().isForbidden();
	}

	private Person createPerson() {
		Person person = new Person("Rodrigo", 23, "admin@gmail.com", "admin", "123", Arrays.asList(new Authority("ROLE_USER")));
		person.setChildren(Arrays.asList(new Child("Daniel", 2), new Child("Oliver", 2)));
		person.setAddress(new Address("50 Main Street", "Bray", "Co. Wicklow", "Ireland", "058 65412"));
		return person;
	}

	private String convertToJson(Person person) throws JsonProcessingException {
		return new ObjectMapper().writeValueAsString(personMapper.map(person));
	}
}
