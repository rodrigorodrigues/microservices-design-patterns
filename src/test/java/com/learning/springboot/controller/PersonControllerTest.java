package com.learning.springboot.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.springboot.config.Java8SpringConfigurationProperties;
import com.learning.springboot.config.SpringSecurityConfiguration;
import com.learning.springboot.config.jwt.TokenProvider;
import com.learning.springboot.dto.PersonDto;
import com.learning.springboot.mapper.PersonMapperImpl;
import com.learning.springboot.model.Person;
import com.learning.springboot.repository.PersonRepository;
import com.learning.springboot.service.PersonService;
import com.learning.springboot.util.HandleResponseError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.web.reactive.function.BodyInserters.fromObject;

@ExtendWith(SpringExtension.class)
@WebFluxTest(properties = {"configuration.initialLoad=false", "debug=true", "logging.level.org.springframework.security=debug"})
@ContextConfiguration(classes = {SpringSecurityConfiguration.class, PersonController.class, HandleResponseError.class})
@EnableConfigurationProperties(Java8SpringConfigurationProperties.class)
public class PersonControllerTest {

    WebTestClient client;

    @Autowired
    ApplicationContext context;

    @Autowired
    Java8SpringConfigurationProperties configurationProperties;

    @MockBean
    PersonService personService;

    @MockBean
    PersonRepository personRepository;

    @MockBean
    TokenProvider tokenProvider;

    final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        Person person = new PersonMapperImpl().dtoToEntity(createPersonDto());
        TokenProvider tokenProvider = new TokenProvider(configurationProperties);
        tokenProvider.init();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(person, "admin", person.getAuthorities());
        String token = "Bearer " + tokenProvider.createToken(authentication, false);
        client = WebTestClient.bindToApplicationContext(context)
                .configureClient()
                .defaultHeader(HttpHeaders.AUTHORIZATION, token)
                .build();
        when(this.tokenProvider.validateToken(anyString())).thenReturn(true);
        when(this.tokenProvider.getAuthentication(anyString())).thenReturn(authentication);
        when(personService.findByUsername(anyString())).thenReturn(Mono.just(person));
    }

    @Test
    @WithMockUser(roles = "READ")
    public void whenCallFindAllShouldReturnListOfPersons() {
        PersonDto person = new PersonDto();
        person.setId("100");
        PersonDto person2 = new PersonDto();
        person2.setId("200");
        when(personService.findAll()).thenReturn(Flux.fromIterable(Arrays.asList(person, person2)));

        ParameterizedTypeReference<ServerSentEvent<PersonDto>> type = new ParameterizedTypeReference<ServerSentEvent<PersonDto>>() {};

        client.get().uri("/api/persons")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM_VALUE)
                .expectBodyList(type)
                .hasSize(2);
    }

    @Test
    @WithMockUser(roles = "READ")
    public void whenCallFindByIdShouldReturnPerson() {
        PersonDto person = new PersonDto();
        person.setId("100");
        when(personService.findById(anyString())).thenReturn(Mono.just(person));

        client.get().uri("/api/persons/{id}", 100)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody().jsonPath("$.id").value(equalTo("100"));
    }

    @Test
    @WithMockUser(roles = "CREATE")
    public void whenCallCreateShouldSavePerson() throws Exception {
        PersonDto personDto = createPersonDto();
        when(personService.save(any(PersonDto.class))).thenReturn(Mono.just(personDto));

        client.post().uri("/api/persons")
                .contentType(MediaType.APPLICATION_JSON)
                .body(fromObject(convertToJson(personDto)))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody().jsonPath("$.id").value(equalTo(personDto.getId()));
    }

    @Test
    @WithMockUser(roles = "SAVE")
    public void whenCallUpdateShouldUpdatePerson() throws Exception {
        PersonDto personDto = createPersonDto();
        personDto.setId(UUID.randomUUID().toString());
        personDto.setName("New Name");
        when(personService.findById(anyString())).thenReturn(Mono.just(personDto));
        when(personService.save(any(PersonDto.class))).thenReturn(Mono.just(personDto));

        client.put().uri("/api/persons/{id}", personDto.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(fromObject(convertToJson(personDto)))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody().jsonPath("$.id").value(equalTo(personDto.getId()))
                .jsonPath("$.name").value(equalTo(personDto.getName()));
    }

    @Test
    @WithMockUser(roles = "DELETE")
    public void whenCallDeleteShouldDeleteById() {
        when(personService.deleteById(anyString())).thenReturn(Mono.empty());
        Person person = new Person();
        person.setId("12345");

        client.delete().uri("/api/persons/{id}", person.getId())
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    private String convertToJson(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    private PersonDto createPersonDto() {
        PersonDto personDto = new PersonDto();
        personDto.setId(UUID.randomUUID().toString());
        personDto.setPassword("{noop}admin");
        personDto.setAge(10);
        personDto.setUsername("admin");
        personDto.setName("Admin");
        personDto.setAuthorities(Arrays.asList(new PersonDto.AuthorityDto("ROLE_ADMIN")));
        personDto.setEmail("admin@gmail.com");
        return personDto;
    }
}