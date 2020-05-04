package com.microservice.quarkus;

import io.quarkus.test.junit.NativeImageTest;

@NativeImageTest
public class NativeCompanyResourceIT extends CompanyResourceTest {

    // Execute the same tests but in native mode.
}