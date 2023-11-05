package com.microservice.person;

import java.io.IOException;
import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.microservice.authentication.autoconfigure.AuthenticationProperties;
import com.microservice.authentication.common.model.Authentication;
import com.microservice.authentication.common.model.Authority;
import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import com.microservice.person.dto.PersonDto;
import com.microservice.person.model.Address;
import com.microservice.person.model.Child;
import com.microservice.person.model.Person;
import com.microservice.person.repository.PersonRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@AutoConfigureDataMongo
@SpringBootTest(classes = PersonServiceApplication.class, properties = "de.flapdoodle.mongodb.embedded.version=5.0.5")
@ContextConfiguration(classes = PersonServiceApplicationIntegrationTest.PopulateDbConfiguration.class)
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
@EmbeddedKafka(partitions = 1, topics = "topic2")
public class PersonServiceApplicationIntegrationTest {

	@Autowired
    MockMvc client;

	@Autowired
    ObjectMapper objectMapper;

	@Autowired
    AuthenticationCommonRepository authenticationRepository;

	@Autowired
    PasswordEncoder passwordEncoder;

	@Autowired
    PersonRepository personRepository;

    @Autowired
    AuthenticationProperties authenticationProperties;

    @Autowired
    EmbeddedKafkaBroker embeddedKafka;

    Consumer<Integer, String> consumer;

	Person person;

	Map<String, List<GrantedAuthority>> users = new HashMap<>();

	AtomicBoolean runAtOnce = new AtomicBoolean(true);

	@TestConfiguration
	static class PopulateDbConfiguration {
        @Primary
        @Bean
        RestTemplate restTemplate() {
            return new RestTemplate();
        }
    }

    {
        users.put("admin@gmail.com", Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        users.put("anonymous@gmail.com", Arrays.asList(new SimpleGrantedAuthority("ROLE_PERSON_READ")));
        users.put("master@gmail.com", Arrays.asList(new SimpleGrantedAuthority("ROLE_PERSON_CREATE"),
            new SimpleGrantedAuthority("ROLE_PERSON_READ"),
            new SimpleGrantedAuthority("ROLE_PERSON_SAVE")));
    }

    @BeforeEach
    public void setup() {
        if (runAtOnce.getAndSet(false)) {
            users.entrySet().stream()
                .map(e -> Authentication.builder().email(e.getKey())
                    .password(passwordEncoder.encode("password123"))
                    .authorities(e.getValue().stream().map(a -> new Authority(a.getAuthority())).collect(Collectors.toList()))
                    .fullName("Master of something")
                    .enabled(true)
                    .build())
                .forEach(authenticationRepository::save);

            personRepository.saveAll(Arrays.asList(Person.builder().fullName("Admin")
                            .dateOfBirth(LocalDate.of(1983, 1, 5))
                            .children(Arrays.asList(new Child("Daniel", LocalDate.of(2017, Month.JANUARY, 1)), new Child("Oliver", LocalDate.of(2017, Month.JANUARY, 1))))
                            .address(new Address("50 Main Street", "Bray", "Co. Wicklow", "Ireland", "058 65412"))
                            .build(),
                    Person.builder().fullName("Anonymous")
                            .dateOfBirth(LocalDate.of(1985, 1, 2))
                            .address(new Address("10 Parnell Street", "Dublin 1", "Dublin", "Ireland", "111 65412"))
                            .build()));
        }

        person = personRepository.save(Person.builder()
            .fullName("Test Master")
            .address(new Address("123 street", "123", "123", "123", "123"))
            .dateOfBirth(LocalDate.now())
            .build());

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testT", "false", embeddedKafka);
        DefaultKafkaConsumerFactory<Integer, String> cf = new DefaultKafkaConsumerFactory<>(
            consumerProps);
        consumer = cf.createConsumer();
        embeddedKafka.consumeFromAllEmbeddedTopics(consumer);
    }

    @AfterEach
    public void tearDown() {
        personRepository.deleteAll();
        consumer.close();
    }

    @Test
	@DisplayName("Test - When Calling GET - /api/people should return filter list of people and response 200 - OK")
	public void shouldReturnListOfPeopleWhenCallApi() throws Exception {
        stubFor(WireMock.get(anyUrl())
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("{\"page\": 0,\"size\": 10,\"totalPages\": 1,\"totalElements\": 2,\"content\":[{\"id\":\"1\",\"name\":\"Post 1\",\"createdDate\": \"2023-10-26 12:24:01\"},{\"id\":\"2\",\"name\":\"Post 2\",\"createdDate\": \"2023-10-26 12:24:01\"}]}")));

        person.setCreatedByUser("master@gmail.com");
	    personRepository.save(person);
		String authorizationHeader = authorizationHeader("master@gmail.com");

		client.perform(get("/api/people")
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader))
				.andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasSize(1)));

        client.perform(get("/api/people")
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].createdByUser", equalTo("master@gmail.com")))
            .andExpect(jsonPath("$.content[0].createdDate", notNullValue()));

        personRepository.saveAll(Arrays.asList(Person.builder()
                .fullName("Test A")
                .address(new Address("123 street", "123", "123", "123", "123"))
                .dateOfBirth(LocalDate.now())
                .createdByUser("master@gmail.com")
                .build(),
                Person.builder()
                    .fullName("Test B")
                    .address(new Address("123 street", "123", "123", "123", "123"))
                    .dateOfBirth(LocalDate.now())
                    .createdByUser("master@gmail.com")
                    .build()
                ));

        client.perform(get("/api/people")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].id", hasSize(3)))
            .andExpect(jsonPath("$.content[*].posts[*].id", hasSize(6)))
            .andExpect(jsonPath("$.content[*].posts[*].createdDate", hasSize(6)))
            .andDo(print());
	}

    @Test
    @DisplayName("Test - When Calling GET - /api/people should return list of people and response 200 - OK")
    public void shouldReturnListOfAllPeopleWhenCallApi() throws Exception {
        String authorizationHeader = authorizationHeader("admin@gmail.com");

        client.perform(get("/api/people")
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements", equalTo(3)))
            .andExpect(jsonPath("$.content[*].id", hasSize(3)));
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/people should return list of people and paging - OK")
    public void shouldReturnListOfAllPeopleAndPagingWhenCallApi() throws Exception {
        String authorizationHeader = authorizationHeader("admin@gmail.com");

        client.perform(get("/api/people?size=2")
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].id", hasSize(2)));
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/people should return list of people using query dsl - OK")
    public void shouldReturnListOfAllPeopleWithQueryDslCallApi() throws Exception {
        String authorizationHeader = authorizationHeader("admin@gmail.com");

        client.perform(get("/api/people?address.address=street")
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].id", hasSize(3)));

        client.perform(get("/api/people?address.address=123")
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].id", hasSize(1)));

        client.perform(get("/api/people?address.address=something else")
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", is(empty())));

        client.perform(get("/api/people?main")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].id", hasSize(1)));
    }

    @Test
    @DisplayName("Test - When Calling POST - /api/people should create a new person and response 201 - Created")
	public void shouldInsertNewPersonWhenCallApi() throws Exception {
		String authorizationHeader = authorizationHeader("master@gmail.com");
		PersonDto person = createPerson();

		String response = client.perform(post("/api/people")
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.content(convertToJson(person)))
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/people/")))
                .andExpect(jsonPath("$.[*].id", notNullValue()))
                .andExpect(jsonPath("$.createdByUser", equalTo("master@gmail.com")))
                .andDo(print())
                .andReturn()
                .getResponse()
                .getContentAsString();

		setId(person, response);

		assertThat(person.getId()).isNotEmpty();

        client.perform(delete("/api/people/{id}", person.getId())
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader("admin@gmail.com")))
            .andExpect(status().is2xxSuccessful())
            .andDo(print());

        ConsumerRecords<Integer, String> consumerRecords = consumer.poll(Duration.ofSeconds(1));
        assertThat(consumerRecords.count()).isEqualTo(2);
	}

    private void setId(PersonDto person, String c) {
        try {
            person.setId(objectMapper.readValue(c, PersonDto.class).getId());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Test - When Calling POST - /api/people without mandatory field should response 400 - Bad Request")
	public void shouldResponseBadRequestWhenCallApiWithoutValidRequest() throws Exception {
		String authorizationHeader = authorizationHeader("admin@gmail.com");

		PersonDto person = createPerson();
		person.setFullName("");

		Exception exception = Assertions.assertThrows(Exception.class, () -> client.perform(post("/api/people")
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.content(convertToJson(person)))
				.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("fullName: size must be between 5 and 200"))));

		assertThat(exception.getCause()).isInstanceOf(ConstraintViolationException.class);
		assertThat(exception.getLocalizedMessage()).contains("fullName: size must be between 5 and 200", "fullName: must not be empty");
	}

	@Test
    @DisplayName("Test - When Calling POST - /api/people without valid authorization should response 403 - Forbidden")
	public void shouldResponseForbiddenWhenCallApiWithoutRightPermission() throws Exception {
		String authorizationHeader = authorizationHeader("anonymous@gmail.com");

		PersonDto person = createPerson();

		client.perform(post("/api/people")
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.content(convertToJson(person)))
				.andExpect(status().isForbidden());
	}

    @Test
    @DisplayName("Test - When Calling GET - /api/people/{id} without valid authorization should response 403 - Forbidden")
    public void shouldResponseForbiddenWhenCallGetApiWithoutRightPermission() throws Exception {
        String authorizationHeader = authorizationHeader("anonymous@gmail.com");
        String id = personRepository.findAll().iterator().next().getId();

        client.perform(get("/api/people/{id}", id)
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message", containsString("User(anonymous@gmail.com) does not have access to this resource")));
    }

	private String authorizationHeader(String user) throws ParseException, JOSEException {
        if (users.containsKey(user)) {
            JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user)
                .expirationTime(Date.from(ZonedDateTime.now().plusMinutes(1).toInstant()))
                .issueTime(new Date())
                .notBeforeTime(new Date())
                .claim("authorities", users.get(user).stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()))
                .jwtID(UUID.randomUUID().toString())
                .issuer("jwt")
                .build();
            JWSSigner signer = new MACSigner(authenticationProperties.getJwt().getKeyValue());
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("kid", "test");
            jsonObject.put("alg", JWSAlgorithm.HS256.getName());
            jsonObject.put("typ", "JWT");
            SignedJWT signedJWT = new SignedJWT(JWSHeader.parse(jsonObject), jwtClaimsSet);
            signedJWT.sign(signer);
            return "Bearer " + signedJWT.serialize();
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
