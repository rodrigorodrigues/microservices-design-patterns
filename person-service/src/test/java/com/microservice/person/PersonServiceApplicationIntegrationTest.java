package com.microservice.person;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.microservice.authentication.common.model.Authentication;
import com.microservice.authentication.common.model.Authority;
import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import com.microservice.person.config.SpringSecurityAuditorAware;
import com.microservice.person.dto.PersonDto;
import com.microservice.person.model.Address;
import com.microservice.person.model.Child;
import com.microservice.person.model.Person;
import com.microservice.person.repository.PersonRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = PersonServiceApplication.class,
		properties = {"configuration.swagger=false",
            "logging.level.com.microservice=debug"})
@ContextConfiguration(initializers = PersonServiceApplicationIntegrationTest.GenerateKeyPairInitializer.class, classes = PersonServiceApplicationIntegrationTest.PopulateDbConfiguration.class)
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
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
    SpringSecurityAuditorAware springSecurityAuditorAware;

    @Autowired
    KeyPair keyPair;

	Person person;

	Map<String, List<GrantedAuthority>> users = new HashMap<>();

	AtomicBoolean runAtOnce = new AtomicBoolean(true);

	@TestConfiguration
	static class PopulateDbConfiguration {
        @Bean
        CommandLineRunner runner(PersonRepository personRepository) {
            return args -> {
                if (personRepository.count() == 0) {
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
            };
        }
    }

    public static class GenerateKeyPairInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

        @SneakyThrows
        @Override
        public void initialize(GenericApplicationContext applicationContext) {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair kp = kpg.generateKeyPair();
            RSAPublicKey pub = (RSAPublicKey) kp.getPublic();
            Key pvt = kp.getPrivate();

            Base64.Encoder encoder = Base64.getEncoder();

            Path privateKeyFile = Files.createTempFile("privateKeyFile", ".key");
            Path publicKeyFile = Files.createTempFile("publicKeyFile", ".cert");

            Files.write(privateKeyFile,
                Arrays.asList("-----BEGIN PRIVATE KEY-----", encoder
                    .encodeToString(pvt.getEncoded()), "-----END PRIVATE KEY-----"));
            log.info("Loaded private key: {}", privateKeyFile);

            Files.write(publicKeyFile,
                Arrays.asList("-----BEGIN PUBLIC KEY-----", encoder
                    .encodeToString(pub.getEncoded()), "-----END PRIVATE KEY-----"));
            log.info("Loaded public key: {}", publicKeyFile);

            applicationContext.registerBean(RSAPublicKey.class, () -> pub);
            applicationContext.registerBean(KeyPair.class, () -> kp);
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
            RSAKey.Builder builder = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyID("test");
            JWKSet jwkSet = new JWKSet(builder.build());

            String jsonPublicKey = jwkSet.toJSONObject().toJSONString();
            stubFor(WireMock.get(urlPathEqualTo("/.well-known/jwks.json"))
                .willReturn(aResponse().withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).withBody(jsonPublicKey)));

            users.entrySet().stream()
                .map(e -> Authentication.builder().email(e.getKey())
                    .password(passwordEncoder.encode("password123"))
                    .authorities(e.getValue().stream().map(a -> new Authority(a.getAuthority())).collect(Collectors.toList()))
                    .fullName("Master of something")
                    .enabled(true)
                    .build())
                .forEach(authenticationRepository::save);
        }

        person = personRepository.save(Person.builder()
            .fullName("Test Master")
            .address(new Address("123 street", "123", "123", "123", "123"))
            .dateOfBirth(LocalDate.now())
            .build());
    }

    @AfterEach
    public void tearDown() {
        personRepository.delete(person);
    }

    @Test
	@DisplayName("Test - When Calling GET - /api/people should return filter list of people and response 200 - OK")
	public void shouldReturnListOfPeopleWhenCallApi() throws Exception {
        person.setCreatedByUser("master@gmail.com");
	    personRepository.save(person);
		String authorizationHeader = authorizationHeader("master@gmail.com");

		client.perform(MockMvcRequestBuilders.get("/api/people")
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader))
				.andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasSize(1)));

        client.perform(MockMvcRequestBuilders.get("/api/people")
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].createdByUser", equalTo("master@gmail.com")))
            .andExpect(jsonPath("$.content[0].createdDate", notNullValue()));
	}

    @Test
    @DisplayName("Test - When Calling GET - /api/people should return list of people and response 200 - OK")
    public void shouldReturnListOfAllPeopleWhenCallApi() throws Exception {
        String authorizationHeader = authorizationHeader("admin@gmail.com");

        client.perform(MockMvcRequestBuilders.get("/api/people")
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements", equalTo(3)))
            .andExpect(jsonPath("$.content[*].id", hasSize(3)));
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/people should return list of people and paging - OK")
    public void shouldReturnListOfAllPeopleAndPagingWhenCallApi() throws Exception {
        String authorizationHeader = authorizationHeader("admin@gmail.com");

        client.perform(MockMvcRequestBuilders.get("/api/people?size=2")
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].id", hasSize(2)));
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/people should return list of people using query dsl - OK")
    public void shouldReturnListOfAllPeopleWithQueryDslCallApi() throws Exception {
        String authorizationHeader = authorizationHeader("admin@gmail.com");

        client.perform(MockMvcRequestBuilders.get("/api/people?address.address=street")
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].id", hasSize(3)));

        client.perform(MockMvcRequestBuilders.get("/api/people?address.address=123")
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].id", hasSize(1)));

        client.perform(MockMvcRequestBuilders.get("/api/people?address.address=something else")
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", is(empty())));
    }

    @Test
    @DisplayName("Test - When Calling POST - /api/people should create a new person and response 201 - Created")
	public void shouldInsertNewPersonWhenCallApi() throws Exception {
		String authorizationHeader = authorizationHeader("master@gmail.com");
		PersonDto person = createPerson();

		String response = client.perform(MockMvcRequestBuilders.post("/api/people")
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.content(convertToJson(person)))
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/people/")))
                .andExpect(jsonPath("$.[*].id", notNullValue()))
                .andExpect(jsonPath("$.createdByUser", equalTo("master@gmail.com")))
                .andReturn()
                .getResponse()
                .getContentAsString();

		setId(person, response);

		assertThat(person.getId()).isNotEmpty();

		client.perform(delete("/api/people/{id}", person.getId())
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader("admin@gmail.com")))
            .andExpect(status().is2xxSuccessful());
	}

    private void setId(PersonDto person, String c) {
        try {
            person.setId(objectMapper.readValue(c, PersonDto.class).getId());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Disabled
    @Test
    @DisplayName("Test - When Calling POST - /api/people without mandatory field should response 400 - Bad Request")
	public void shouldResponseBadRequestWhenCallApiWithoutValidRequest() throws Exception {
		String authorizationHeader = authorizationHeader("admin@gmail.com");

		PersonDto person = createPerson();
		person.setFullName("");

		client.perform(MockMvcRequestBuilders.post("/api/people")
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.content(convertToJson(person)))
				.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("fullName: size must be between 5 and 200")));
	}

	@Test
    @DisplayName("Test - When Calling POST - /api/people without valid authorization should response 403 - Forbidden")
	public void shouldResponseForbiddenWhenCallApiWithoutRightPermission() throws Exception {
		String authorizationHeader = authorizationHeader("anonymous@gmail.com");

		PersonDto person = createPerson();

		client.perform(MockMvcRequestBuilders.post("/api/people")
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.content(convertToJson(person)))
				.andExpect(status().isForbidden());
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
            JWSSigner signer = new RSASSASigner(keyPair.getPrivate());
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("kid", "test");
            jsonObject.put("alg", JWSAlgorithm.RS256.getName());
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
