package com.learning.springboot.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.springboot.config.Java8SpringConfigurationProperties;
import com.learning.springboot.config.SpringSecurityConfiguration;
import com.learning.springboot.config.jwt.TokenProvider;
import com.learning.springboot.dto.PersonDto;
import com.learning.springboot.model.Person;
import com.learning.springboot.service.PersonService;
import com.learning.springboot.util.HandleResponseError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.web.reactive.function.BodyInserters.fromObject;

@ExtendWith(SpringExtension.class)
@WebFluxTest(properties = {
        "configuration.initialLoad=false",
        "configuration.mongo=false",
        "debug=true",
        "logging.level.org.springframework=debug"},
controllers = PersonController.class, excludeAutoConfiguration = MongoReactiveAutoConfiguration.class)
@Import({SpringSecurityConfiguration.class, HandleResponseError.class, ErrorWebFluxAutoConfiguration.class})
@EnableConfigurationProperties(Java8SpringConfigurationProperties.class)
public class PersonControllerTest {

    @Autowired
    WebTestClient client;

    @Autowired
    Java8SpringConfigurationProperties configurationProperties;

    @MockBean
    PersonService personService;

    @MockBean
    TokenProvider tokenProvider;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken("admin@gmail.com", "admin", Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(tokenProvider.validateToken(anyString())).thenReturn(true);
        when(tokenProvider.getAuthentication(anyString())).thenReturn(authentication);
    }

    @Test
    @DisplayName("Test - When Cal GET - /api/persons without valid authorization the response should be 403 - Forbidden")
    @WithMockUser(roles = "INVALID_ROLE")
    public void whenCallFindAllShouldReturnForbiddenWhenDoesNotHavePermission() {
        client.get().uri("/api/persons")
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Test - When Cal GET - /api/persons without authorization the response should be 401 - Unauthorized")
    public void whenCallFindAllShouldReturnUnauthorizedWhenDoesNotHavePermission() {
        client.get().uri("/api/persons")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Test - When Cal GET - /api/persons with valid authorization the response should be a list of People - 200 - OK")
    @WithMockUser(roles = "PERSON_READ")
    public void whenCallFindAllShouldReturnListOfPersons() {
        PersonDto person = new PersonDto();
        person.setId("100");
        PersonDto person2 = new PersonDto();
        person2.setId("200");
        when(personService.findAll()).thenReturn(Flux.fromIterable(Arrays.asList(person, person2)));

        ParameterizedTypeReference<ServerSentEvent<PersonDto>> type = new ParameterizedTypeReference<ServerSentEvent<PersonDto>>() {};

        client.get().uri("/api/persons")
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM_VALUE)
                .expectBodyList(type)
                .hasSize(2);
    }

    @Test
    @DisplayName("Test - When Cal GET - /api/persons/{id} with valid authorization the response should be user - 200 - OK")
    @WithMockUser(roles = "PERSON_READ")
    public void whenCallFindByIdShouldReturnPerson() {
        PersonDto person = new PersonDto();
        person.setId("100");
        when(personService.findById(anyString())).thenReturn(Mono.just(person));

        client.get().uri("/api/persons/{id}", 100)
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody().jsonPath("$.id").value(equalTo("100"));
    }

    @Test
    @DisplayName("Test - When Cal POST - /api/persons with valid authorization the response should be a user - 201 - Created")
    @WithMockUser(roles = "PERSON_CREATE")
    public void whenCallCreateShouldSavePerson() throws Exception {
        PersonDto personDto = createPersonDto();
        when(personService.save(any(PersonDto.class))).thenReturn(Mono.just(personDto));

        client.post().uri("/api/persons")
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .contentType(MediaType.APPLICATION_JSON)
                .body(fromObject(convertToJson(personDto)))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody().jsonPath("$.id").value(equalTo(personDto.getId()));
    }

    @Test
    @DisplayName("Test - When Cal PUT - /api/persons/{id} with valid authorization the response should be a user - 200 - OK")
    @WithMockUser(roles = "PERSON_SAVE")
    public void whenCallUpdateShouldUpdatePerson() throws Exception {
        PersonDto personDto = createPersonDto();
        personDto.setId(UUID.randomUUID().toString());
        personDto.setFullName("New Name");
        when(personService.findById(anyString())).thenReturn(Mono.just(personDto));
        when(personService.save(any(PersonDto.class))).thenReturn(Mono.just(personDto));

        client.put().uri("/api/persons/{id}", personDto.getId())
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .contentType(MediaType.APPLICATION_JSON)
                .body(fromObject(convertToJson(personDto)))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody().jsonPath("$.id").value(equalTo(personDto.getId()))
                .jsonPath("$.fullName").value(equalTo(personDto.getFullName()));
    }

    @Test
    @DisplayName("Test - When Cal PUT - /api/persons/{id} with invalid id the response should 404 - Not Found")
    @WithMockUser(roles = "PERSON_SAVE")
    public void whenCallUpdateShouldResponseNotFound() throws Exception {
        PersonDto personDto = createPersonDto();
        personDto.setId("999");
        when(personService.findById(anyString())).thenReturn(Mono.empty());

        client.put().uri("/api/persons/{id}", personDto.getId())
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .contentType(MediaType.APPLICATION_JSON)
                .body(fromObject(convertToJson(personDto)))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Test - When Cal DELETE - /api/persons/{id} with valid authorization the response should be 200 - OK")
    @WithMockUser(roles = "PERSON_DELETE")
    public void whenCallDeleteShouldDeleteById() {
        when(personService.deleteById(anyString())).thenReturn(Mono.empty());
        Person person = new Person();
        person.setId("12345");

        client.delete().uri("/api/persons/{id}", person.getId())
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    private String convertToJson(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    private PersonDto createPersonDto() {
        return PersonDto.builder()
                .id(UUID.randomUUID().toString())
                .dateOfBirth(LocalDate.now())
                .fullName("Admin")
                .build();
    }
}