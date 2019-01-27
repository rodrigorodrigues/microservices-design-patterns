package com.learning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.springboot.PersonServiceApplication;
import com.learning.springboot.config.jwt.TokenProvider;
import com.learning.springboot.dto.PersonDto;
import com.learning.springboot.mapper.PersonMapper;
import com.learning.springboot.model.Authority;
import com.learning.springboot.model.User;
import com.learning.springboot.repository.PersonRepository;
import com.learning.springboot.repository.UserRepository;
import com.learning.springboot.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.web.reactive.function.BodyInserters.fromObject;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = PersonServiceApplication.class,
		properties = {"configuration.swagger=false", "debug=true"})
@ActiveProfiles("integration-tests")
@AutoConfigureWebTestClient
@Import(PersonServiceApplicationIntegrationTest.UserMockConfiguration.class)
public class PersonServiceApplicationIntegrationTest {

	@Autowired
	ApplicationContext context;

	@Autowired
    PersonMapper personMapper;

	@Autowired
    PersonRepository personRepository;

	@Autowired
	WebTestClient client;

	@Autowired
    ObjectMapper objectMapper;

	@Autowired
	TokenProvider tokenProvider;

	@Autowired
	UserService userService;

	@Configuration
    static class UserMockConfiguration {
	    @Autowired
        UserRepository userRepository;

	    @Autowired
        PasswordEncoder passwordEncoder;

	    @PostConstruct
        public void init() {
            userRepository.save(User.builder().email("admin@gmail.com")
                .password(passwordEncoder.encode("password"))
                .authorities(permissions("ROLE_ADMIN"))
                .fullName("Admin dos Santos")
                .build()).subscribe(u -> System.out.println(String.format("Created Admin User: %s", u)));

            userRepository.save(User.builder().email("anonymous@gmail.com")
                .password(passwordEncoder.encode("test"))
                .authorities(permissions("ROLE_READ"))
                .fullName("Anonymous Noname")
                .build()).subscribe(u -> System.out.println(String.format("Created Anonymous User: %s", u)));

            userRepository.save(User.builder().email("master@gmail.com")
                .password(passwordEncoder.encode("password123"))
                .authorities(permissions("ROLE_CREATE", "ROLE_READ", "ROLE_SAVE"))
                .fullName("Master of something")
                .build()).subscribe(u -> System.out.println(String.format("Created Master User: %s", u)));

        }

        private List<Authority> permissions(String ... permissions) {
            return Stream.of(permissions)
                .map(Authority::new)
                .collect(Collectors.toList());
        }
    }

    @Test
	@DisplayName("Test - When Cal GET - /api/persons should return list of people and response 200 - OK")
	public void shouldReturnListOfPersonsWhenCallApi() {
		String authorizationHeader = authenticate("master@gmail.com", "password123");

		client.get().uri("/api/persons")
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader)
				.exchange()
				.expectStatus().isOk();
	}

	@Test
    @DisplayName("Test - When Cal POST - /api/persons should create a new user and response 201 - Created")
	public void shouldInsertNewPersonWhenCallApi() throws Exception {
		String authorizationHeader = authenticate("master@gmail.com", "password123");
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
                    .jsonPath("$.id").isNotEmpty();
                    //.jsonPath("$.createdByUser.email").isEqualTo("master@gmail.com");
	}

	@Test
    @DisplayName("Test - When Cal POST - /api/persons without mandatory field should response 400 - Bad Request")
	public void shouldResponseBadRequestWhenCallApiWithoutValidRequest() throws JsonProcessingException {
		String authorizationHeader = authenticate("admin@gmail.com", "password");

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
		String authorizationHeader = authenticate("anonymous@gmail.com", "test");

		PersonDto person = createPerson();

		client.post().uri("/api/persons")
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.body(fromObject(convertToJson(person)))
				.exchange()
				.expectStatus().isForbidden();
	}

	private String authenticate(String user, String password) {
		return userService.findByUsername(user)
				.map(u -> tokenProvider.createToken(new UsernamePasswordAuthenticationToken(u, password, u.getAuthorities()), "Something", false))
				.map(t -> "Bearer " + t)
				.block();
	}

	private PersonDto createPerson() {
		return PersonDto.builder().fullName("Rodrigo")
			.dateOfBirth(LocalDate.of(1988, 1, 1))
			.children(Arrays.asList(new PersonDto.ChildrenDto("Daniel", 2), new PersonDto.ChildrenDto("Oliver", 2)))
			.address(new PersonDto.Address(null, "50 Main Street", "Bray", "Co. Wicklow", "Ireland", "058 65412"))
			.build();
	}

	private String convertToJson(PersonDto person) throws JsonProcessingException {
        return objectMapper.writeValueAsString(person);
	}

}
