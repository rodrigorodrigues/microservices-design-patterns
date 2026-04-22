# Design Spec - Integration Tests for Scala Address Service

## Overview
This document outlines the design for adding integration tests to the `scala-address-service`. The goal is to verify the service's interaction with a real MongoDB database and ensure that the Pekko HTTP routes correctly integrate with the Spring Data MongoDB repository.

## Problem Statement
The current test suite for `scala-address-service` consists only of unit tests with mocked repositories. While useful, they don't catch issues related to database schema mismatch, query logic errors, or integration between Pekko and Spring.

## Proposed Changes

### 1. Dependency Updates (`pom.xml`)
Add the following dependencies to support integration testing with Testcontainers:
- `org.springframework.boot:spring-boot-starter-test`
- `org.springframework.boot:spring-boot-testcontainers`
- `org.testcontainers:mongodb`

### 2. Infrastructure
Create a shared Testcontainers configuration for MongoDB in Scala.

- **File**: `src/test/scala/com/microservice/address/TestcontainersConfiguration.scala`
- **Content**: A `@TestConfiguration` class that defines a `@ServiceConnection` for `MongoDBContainer`.

### 3. Repository Integration Test
Verify that `AddressRepository` works correctly with a real MongoDB instance.

- **File**: `src/test/scala/com/microservice/address/repository/AddressRepositoryIT.scala`
- **Annotation**: `@DataMongoTest`
- **Tests**: CRUD operations (Save, Find, Delete).

### 4. End-to-End Service Integration Test
Verify the full request-response cycle from Pekko HTTP routes down to the MongoDB database.

- **File**: `src/test/scala/com/microservice/address/AddressServiceIT.scala`
- **Annotation**: `@SpringBootTest`
- **Approach**:
    - Start the Spring context.
    - Manually bind the Pekko HTTP server to a random port (or use `ScalatestRouteTest` with the Spring-managed repository).
    - Send real HTTP requests using Pekko HTTP client or `ScalatestRouteTest`.
    - Verify data persistence in the MongoDB container.

## Verification Plan
1. Run `mvn clean verify -pl scala-address-service`.
2. Ensure both unit and integration tests pass.
3. Verify that the MongoDB container starts and stops as expected during the test run.
