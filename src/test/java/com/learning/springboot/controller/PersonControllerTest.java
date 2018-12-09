package com.learning.springboot.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.springboot.dto.PersonDto;
import com.learning.springboot.mapper.PersonMapper;
import com.learning.springboot.mapper.PersonMapperImpl;
import com.learning.springboot.model.Person;
import com.learning.springboot.repository.PersonRepository;
import com.learning.springboot.service.PersonService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@WebMvcTest(PersonController.class)
public class PersonControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    PersonService personService;

    @MockBean
    PersonMapper personMapper;

    @MockBean
    PersonRepository personRepository;

    final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @WithMockUser(roles = "READ")
    public void whenCallFindAllShouldReturnListOfPersons() throws Exception {
        Person person = new Person();
        person.setId("100");
        Person person2 = new Person();
        person2.setId("200");
        when(personService.findAll()).thenReturn(Arrays.asList(person, person2));
        when(personMapper.entityToDto(any(List.class))).thenReturn(Arrays.asList(person, person2));

        mockMvc.perform(get("/persons"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.[*].id").value(hasItems("100", "200")));
    }

    @Test
    @WithMockUser(roles = "READ")
    public void whenCallFindByIdShouldReturnPerson() throws Exception {
        PersonDto person = new PersonDto();
        person.setId("100");
        when(personService.findById(anyString())).thenReturn(Optional.of(new Person()));
        when(personMapper.entityToDto(any(Person.class))).thenReturn(person);

        mockMvc.perform(get("/persons/{id}", 100))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("100"));
    }

    @Test
    @WithMockUser(roles = "CREATE")
    public void whenCallCreateShouldSavePerson() throws Exception {
        PersonDto personDto = createPersonDto();
        Person person = new Person();
        when(personMapper.dtoToEntity(any(PersonDto.class))).thenReturn(person);
        when(personService.save(any(Person.class))).thenReturn(person);

        mockMvc.perform(post("/persons")
                .content(convertToJson(personDto))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(person.getId()));
    }

    @Test
    @WithMockUser(roles = "SAVE")
    public void whenCallUpdateShouldUpdatePerson() throws Exception {
        PersonDto personDto = createPersonDto();
        personDto.setId(UUID.randomUUID().toString());
        personDto.setName("New Name");
        Person person = new PersonMapperImpl().dtoToEntity(personDto);
        when(personService.findById(anyString())).thenReturn(Optional.of(person));

        mockMvc.perform(put("/persons/{id}", personDto.getId())
                .content(convertToJson(personDto))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(person.getId()))
                .andExpect(jsonPath("$.name").value(personDto.getName()));
    }

    @Test
    @WithMockUser(roles = "DELETE")
    public void whenCallDeleteShouldDeleteById() throws Exception {
        Person person = new Person();

        mockMvc.perform(delete("/persons/{id}", person.getId()))
                .andExpect(status().is2xxSuccessful());
    }

    private String convertToJson(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    private PersonDto createPersonDto() {
        PersonDto personDto = new PersonDto();
        personDto.setPassword("admin");
        personDto.setAge(10);
        personDto.setLogin("admin");
        personDto.setName("Admin");
        personDto.setAuthorities(Arrays.asList(new PersonDto.AuthorityDto("ADMIN")));
        return personDto;
    }
}