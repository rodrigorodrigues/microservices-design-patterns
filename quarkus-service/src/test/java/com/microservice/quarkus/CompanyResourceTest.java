package com.microservice.quarkus;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
public class CompanyResourceTest {
    @BeforeAll
    public static void init() {
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/companies with admin role the response should response all companies - 200 - OK")
    public void testGetAllCompanies() {
        given()
          .when()
                .auth().preemptive().basic("admin", "admin")
                .get("/api/companies")
          .then()
             .statusCode(200)
             .body("$.size()", is(3),
                     "name", hasItems("Facebook", "Google", "Amazon"));
    }

}