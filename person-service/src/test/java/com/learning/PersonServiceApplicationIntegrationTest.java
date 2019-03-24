package com.learning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.springboot.PersonServiceApplication;
import com.learning.springboot.config.jwt.TokenProvider;
import com.learning.springboot.dto.PersonDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.web.reactive.function.BodyInserters.fromObject;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = PersonServiceApplication.class,
		properties = "configuration.swagger=false")
@ActiveProfiles("integration-tests")
@AutoConfigureWebTestClient
public class PersonServiceApplicationIntegrationTest {

	@Autowired
	WebTestClient client;

	@Autowired
    ObjectMapper objectMapper;

	@Autowired
	TokenProvider tokenProvider;

	Map<String, List<GrantedAuthority>> users = new HashMap<>();

    {
        users.put("admin@gmail.com", Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        users.put("anonymous@gmail.com", Arrays.asList(new SimpleGrantedAuthority("ROLE_PERSON_READ")));
        users.put("master@gmail.com", Arrays.asList(new SimpleGrantedAuthority("ROLE_PERSON_CREATE"),
            new SimpleGrantedAuthority("ROLE_PERSON_READ"),
            new SimpleGrantedAuthority("ROLE_PERSON_SAVE")));
    }

    @Test
	@DisplayName("Test - When Cal GET - /api/persons should return list of people and response 200 - OK")
	public void shouldReturnListOfPersonsWhenCallApi() {
		String authorizationHeader = authorizationHeader("master@gmail.com");

		client.get().uri("/api/persons")
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader)
				.exchange()
				.expectStatus().isOk();
	}

	@Test
    @DisplayName("Test - When Cal POST - /api/persons should create a new person and response 201 - Created")
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
                    .jsonPath("$.createdByUser").isEqualTo("master@gmail.com");
	}

	@Test
    @DisplayName("Test - When Cal POST - /api/persons without mandatory field should response 400 - Bad Request")
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
    @DisplayName("Test - When Cal POST - /api/persons without valid authorization should response 403 - Forbidden")
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
            return "Bearer " + tokenProvider.createToken(new UsernamePasswordAuthenticationToken(user, null, users.get(user)), "Something", false);
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
