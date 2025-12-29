package com.microservice.quarkus;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Disabled;

@Disabled
@QuarkusIntegrationTest
public class NativeCompanyResourceIT extends CompanyResourceTest {

    // Execute the same tests but in native mode.
}