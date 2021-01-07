package com.microservice.person.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.authentication.autoconfigure.AuthenticationCommonConfiguration;
import com.microservice.person.config.SpringSecurityAuditorAware;
import com.microservice.person.dto.PersonDto;
import com.microservice.person.repository.PersonRepository;
import com.microservice.person.service.PersonService;
import com.microservice.web.autoconfigure.WebCommonAutoConfiguration;
import com.querydsl.core.types.Predicate;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(properties = {
        "configuration.initialLoad=false",
        "configuration.mongo=false"},
controllers = PersonController.class, excludeAutoConfiguration = MongoAutoConfiguration.class)
@Import({AuthenticationCommonConfiguration.class, WebCommonAutoConfiguration.class})
public class PersonControllerTest {

    @Autowired
    MockMvc client;

    @MockBean
    PersonService personService;

    @MockBean
    TokenStore tokenStore;

    @MockBean
    SpringSecurityAuditorAware springSecurityAuditorAware;

    @Autowired
    ObjectMapper objectMapper;

    @TestConfiguration
    static class MockConfiguration {

        @Bean
        public PersonRepository personRepository() {
            return mock(PersonRepository.class);
        }

        @Bean
        public JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }

    }

    @Test
    @DisplayName("Test - When Calling GET - /api/people without valid authorization the response should be 403 - Forbidden")
    @WithMockUser(roles = "INVALID_ROLE")
    public void whenCallFindAllShouldReturnForbiddenWhenDoesNotHavePermission() throws Exception {
        client.perform(MockMvcRequestBuilders.get("/api/people")
            .header(HttpHeaders.AUTHORIZATION, "MOCK JWT"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/people without authorization the response should be 401 - Unauthorized")
    public void whenCallFindAllShouldReturnUnauthorizedWhenDoesNotHavePermission() throws Exception {
        client.perform(MockMvcRequestBuilders.get("/api/people"))
            .andExpect(status().isUnauthorized());

    }

    @Test
    @DisplayName("Test - When Calling GET - /api/people with different user should return empty list and response 200 - OK")
    @WithMockUser(roles = "PERSON_READ", username = "test")
    public void whenCallFindAllShouldReturnEmptyList() throws Exception {
        when(personService.findAllByCreatedByUser(anyString(), any(Pageable.class), any(Predicate.class))).thenReturn(new PageImpl(Collections.EMPTY_LIST));

        client.perform(MockMvcRequestBuilders.get("/api/people")
            .header(HttpHeaders.AUTHORIZATION, "MOCK JWT"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content", is(empty())));
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/people with admin role the response should be a list of People - 200 - OK")
    @WithMockUser(roles = "ADMIN")
    public void whenCallFindAllShouldReturnListOfPersons() throws Exception {
        PersonDto person = new PersonDto();
        person.setId("100");
        PersonDto person2 = new PersonDto();
        person2.setId("200");
        when(personService.findAll(any(Pageable.class), any(Predicate.class))).thenReturn(new PageImpl(Arrays.asList(person, person2)));

        client.perform(MockMvcRequestBuilders.get("/api/people")
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$..id", hasSize(2)));
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/people with person_read role the response should be filtered - 200 - OK")
    @WithMockUser(roles = "PERSON_READ", username = "me")
    public void whenCallShouldFilterListOfPersons() throws Exception {
        PersonDto person = new PersonDto();
        person.setId("100");
        person.setCreatedByUser("me");
        when(personService.findAllByCreatedByUser(anyString(), any(Pageable.class), any(Predicate.class))).thenReturn(new PageImpl(Collections.singletonList(person)));

        client.perform(MockMvcRequestBuilders.get("/api/people")
            .header(HttpHeaders.AUTHORIZATION, "MOCK JWT"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$..id", hasSize(1)));
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/people/{id} with valid authorization the response should be person - 200 - OK")
    @WithMockUser(roles = "PERSON_READ", username = "me")
    public void whenCallFindByIdShouldReturnPerson() throws Exception {
        PersonDto person = new PersonDto();
        person.setId("100");
        person.setCreatedByUser("me");
        when(personService.findById(anyString())).thenReturn(person);

        client.perform(MockMvcRequestBuilders.get("/api/people/{id}", 100)
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is("100")));
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/people/{id} with different user should response 403 - Forbidden")
    @WithMockUser(roles = "PERSON_READ", username = "test")
    public void whenCallFindByIdShouldResponseForbidden() throws Exception {
        PersonDto person = new PersonDto();
        person.setId("100");
        person.setCreatedByUser("test1");
        when(personService.findById(anyString())).thenReturn(person);

        client.perform(MockMvcRequestBuilders.get("/api/people/{id}", 100)
            .header(HttpHeaders.AUTHORIZATION, "MOCK JWT"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message", containsString("User(test) does not have access to this resource")));
    }

    @Test
    @DisplayName("Test - When Calling POST - /api/people with valid authorization the response should be a person - 201 - Created")
    @WithMockUser(roles = "PERSON_CREATE")
    public void whenCallCreateShouldSavePerson() throws Exception {
        PersonDto personDto = createPersonDto();
        when(personService.save(any(PersonDto.class))).thenReturn(personDto);

        client.perform(MockMvcRequestBuilders.post("/api/people")
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertToJson(personDto)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(personDto.getId())));
    }

    @Test
    @DisplayName("Test - When Calling PUT - /api/people/{id} with valid authorization the response should be a person - 200 - OK")
    @WithMockUser(roles = "PERSON_SAVE", username = "me")
    public void whenCallUpdateShouldUpdatePerson() throws Exception {
        PersonDto personDto = createPersonDto();
        personDto.setId(UUID.randomUUID().toString());
        personDto.setFullName("New Name");
        when(personService.findById(anyString())).thenReturn(personDto);
        when(personService.save(any(PersonDto.class))).thenReturn(personDto);

        client.perform(MockMvcRequestBuilders.put("/api/people/{id}", personDto.getId())
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertToJson(personDto)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(personDto.getId())))
                .andExpect(jsonPath("$.fullName", is(personDto.getFullName())));
    }

    @Test
    @DisplayName("Test - When Calling PUT - /api/people/{id} with invalid id the response should be 404 - Not Found")
    @WithMockUser(roles = "PERSON_SAVE", username = "me")
    public void whenCallUpdateShouldResponseNotFound() throws Exception {
        PersonDto personDto = createPersonDto();
        personDto.setId("999");
        when(personService.findById(anyString())).thenReturn(null);

        client.perform(put("/api/people/{id}", personDto.getId())
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertToJson(personDto)))
                .andExpect(status().isNotFound());
    }

    @SneakyThrows
    @Test
    @DisplayName("Test - When Calling DELETE - /api/people/{id} with valid authorization the response should be 200 - OK")
    @WithMockUser(roles = "PERSON_DELETE", username = "mock")
    public void whenCallDeleteShouldDeleteById() {
        PersonDto person = new PersonDto();
        person.setId("12345");
        person.setCreatedByUser("mock");
        when(personService.findById(anyString())).thenReturn(person);

        client.perform(delete("/api/people/{id}", person.getId())
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DisplayName("Test - When Calling DELETE - /api/people/{id} with different user  the response should be 403 - Forbidden")
    @WithMockUser(roles = "PERSON_DELETE", username = "test")
    public void whenCallDeleteWithDifferentUSerShouldResponseForbidden() throws Exception {
        PersonDto person = new PersonDto();
        person.setId("12345");
        person.setCreatedByUser("mock");
        when(personService.findById(anyString())).thenReturn(person);

        client.perform(delete("/api/people/{id}", person.getId())
            .header(HttpHeaders.AUTHORIZATION, "MOCK JWT"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message", containsString("User(test) does not have access to delete this resource")));
    }

    @Test
    @DisplayName("Test - When Calling DELETE - /api/people/{id} with id that does not exist should response 404 - Not Found")
    @WithMockUser(roles = "PERSON_DELETE")
    public void whenCallDeleteShouldResponseNotFound() throws Exception {
        when(personService.findById(anyString())).thenReturn(null);

        client.perform(delete("/api/people/{id}", "12345")
            .header(HttpHeaders.AUTHORIZATION, "MOCK JWT"))
            .andExpect(status().isNotFound());

        verify(personService, never()).deleteById(anyString());
    }

    private String convertToJson(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    private PersonDto createPersonDto() {
        return PersonDto.builder()
                .id(UUID.randomUUID().toString())
                .dateOfBirth(LocalDate.now())
                .createdByUser("me")
                .fullName("Admin")
                .build();
    }
}
