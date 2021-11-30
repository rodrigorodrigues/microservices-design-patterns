package com.microservice.quarkus;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.microservice.quarkus.dto.CompanyDto;
import com.microservice.quarkus.model.Company;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
public class CompanyResourceTest {
    @Inject
    AppLifecycleBean appLifecycleBean;

    @Inject
    Vertx vertx;

    @ConfigProperty(name = "quarkus.http.test-port")
    Integer assignedPort;

    WebClient client;

    @BeforeAll
    public static void init() {
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
    }

    @PostConstruct
    public void initializeWebClient() {
        this.client = WebClient.create(vertx, new WebClientOptions()
                .setDefaultPort(assignedPort)
                .setLogActivity(true));
    }

    @BeforeEach
    public void setup() {
        Company.deleteAll().await().indefinitely();
        appLifecycleBean.onStart(Mockito.mock(StartupEvent.class));
    }

    @Test
    @DisplayName("Test - When Calling DELETE - /api/companies/{id} with admin user should delete resource - 204")
    public void testDeleteCompany() {
        Company company = new Company();
        company.name = "Test";
        company.createdByUser = "admin";
        company.persist().await().indefinitely();

        given()
                .when()
                .auth().preemptive().basic("admin", "admin")
                .delete("/api/companies/{id}", company.id.toHexString())
                .then()
                .statusCode(204);
    }

    @Test
    @DisplayName("Test - When Calling POST - /api/companies with admin user should create resource - 204")
    public void testCreateCompany() {
        CompanyDto companyDto = new CompanyDto();
        companyDto.setName("new company");

        given()
                .when()
                .auth().preemptive().basic("admin", "admin")
                .body(companyDto)
                .contentType(ContentType.JSON)
                .post("/api/companies")
                .then()
                .statusCode(201)
                .header(HttpHeaders.LOCATION.toString(), containsString("/api/companies/"))
                .body("createdDate", is(notNullValue()))
                .body("lastModifiedDate", is(notNullValue()))
                .body("createdByUser", is("admin"))
                .body("name", is("new company"));
    }

    @Test
    @DisplayName("Test - When Calling DELETE - /api/companies/{id} with test user should response 403 - Forbidden")
    public void testDeleteCompanyWithoutRoleShouldResponseForbidden() {
        Company company = new Company();
        company.name = "Test";
        company.createdByUser = "test";
        company.persist().await().indefinitely();

        given()
                .when()
                .auth().preemptive().basic("test", "test")
                .delete("/api/companies/{id}", company.id.toHexString())
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/companies with test user should filter the response - 200 - OK")
    public void testGetAllCompaniesForSpecificUser() {
        String json = client.get("/api/companies")
                .basicAuthentication("test", "test")
                .send()
                .onItem().transform(res -> {
                    String body = StringUtils.trimToEmpty(res.bodyAsString());
                    if (StringUtils.isBlank(body)) {
                        return null;
                    } else {
                        return body;
                    }
                })
                .await().indefinitely();

        assertNull(json);
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/companies with admin user the response should response all companies - 200 - OK")
    public void testGetAllCompanies() {
        JsonPath json = client.get("/api/companies")
                .basicAuthentication("admin", "admin")
                .send()
                .onItem().transform(res -> {
                    String list = Stream.of(res.bodyAsString().split("data: "))
                            .map(String::trim)
                            .filter(StringUtils::isNotBlank)
                            .collect(Collectors.joining(","));
                    return new JsonPath("["+list+"]");
                })
                .await().indefinitely();

        assertNotNull(json);
        List<String> names = json.getList("name");
        assertThat(names.size(), is(3));
        assertThat(names, hasItems("Facebook", "Google", "Amazon"));
    }

}