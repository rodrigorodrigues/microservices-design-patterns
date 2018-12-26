package com.learning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.springboot.Java8SpringBootApplication;
import com.learning.springboot.config.jwt.TokenProvider;
import com.learning.springboot.mapper.PersonMapper;
import com.learning.springboot.model.Address;
import com.learning.springboot.model.Authority;
import com.learning.springboot.model.Child;
import com.learning.springboot.model.Person;
import com.learning.wsdl.client.Asset;
import com.learning.wsdl.client.AssetStatus;
import com.learning.wsdl.client.CreateOrUpdateAsset;
import com.learning.wsdl.client.ObjectFactory;
import org.hamcrest.CustomMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.xml.transform.StringResult;

import javax.validation.ConstraintViolationException;
import javax.xml.bind.JAXBElement;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Java8SpringBootApplication.class,
		properties = {"initialLoad=false", "debug=true"}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class Java8SpringBootApplicationIntegrationTest {

	@Autowired
	WebApplicationContext context;

	@Autowired
	PersonMapper personMapper;

	@Autowired
	TokenProvider tokenProvider;

	MockMvc mockMvc;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Before
	public void setup() {
		Person person = createPerson();
		String token = "Bearer " + tokenProvider.createToken(new UsernamePasswordAuthenticationToken(person, null, person.getAuthorities()), false);
		mockMvc = MockMvcBuilders.webAppContextSetup(context)
				.apply(springSecurity())
				.defaultRequest(MockMvcRequestBuilders.get("/").header(HttpHeaders.AUTHORIZATION, token))
				.build();
	}

	@Test
	public void testWsdlClient() {
		ObjectFactory objectFactory = new ObjectFactory();
		CreateOrUpdateAsset createOrUpdateAsset = objectFactory.createCreateOrUpdateAsset();
		Asset asset = objectFactory.createAsset();
		AssetStatus assetStatus = objectFactory.createAssetStatus();
		assetStatus.setAssetStatusId(111);
		asset.setAssetStatus(assetStatus);
		createOrUpdateAsset.setArg0(asset);

		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setContextPath("com.learning.wsdl.client");

		JAXBElement<CreateOrUpdateAsset> jaxbElement = objectFactory.createCreateOrUpdateAsset(createOrUpdateAsset);

		StringResult result = new StringResult();
		marshaller.marshal(jaxbElement, result);

		assertThat(result.toString()).isNotEmpty();
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	public void shouldInsertNewPersonWhenCallApi() throws Exception {
		Person person = createPerson();

		mockMvc.perform(post("/api/persons")
				.contentType(MediaType.APPLICATION_JSON)
				.content(convertToJson(person)))
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/persons/")))
				.andExpect(jsonPath("$.id").isNotEmpty());
	}

	@Test
	@WithMockUser(roles = "CREATE")
	public void shouldResponseBadRequestWhenCallApiWithoutValidRequest() throws Exception {
		expectedException.expectCause(new CustomMatcher<Throwable>("Check Exception") {
			@Override
			public boolean matches(Object item) {
				return item.getClass().equals(ConstraintViolationException.class);
			}
		}); //TODO Check later how to use json response instead exception.

        Person person = createPerson();
		person.setName("");

		mockMvc.perform(post("/api/persons")
				.contentType(MediaType.APPLICATION_JSON)
				.content(convertToJson(person)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors").isNotEmpty());
	}

	@Test
	@WithMockUser(roles = "READ")
	public void shouldResponseForbiddenWhenCallApiWithoutRightPermission() throws Exception {
		Person person = createPerson();

		mockMvc.perform(post("/api/persons")
				.contentType(MediaType.APPLICATION_JSON)
				.content(convertToJson(person)))
				.andExpect(status().isForbidden());
	}

	private Person createPerson() {
		Person person = new Person("Rodrigo", 23, "admin", "123", Arrays.asList(new Authority("USER")));
		person.setChildren(Arrays.asList(new Child("Daniel", 2), new Child("Oliver", 2)));
		person.setAddress(new Address("50 Main Street", "Bray", "Co. Wicklow", "Ireland", "058 65412"));
		return person;
	}

	private String convertToJson(Person person) throws JsonProcessingException {
		return new ObjectMapper().writeValueAsString(personMapper.entityToDto(person));
	}
}
