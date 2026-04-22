# Design Spec - Comprehensive Testing for scala-address-service

## Status
- **Date**: 2026-04-17
- **Status**: Draft
- **Author**: Junie

## Problem Statement
The `scala-address-service` currently has only one trivial test, which does not provide adequate code coverage. The user expects proper code coverage for each class in the service.

## Proposed Changes
We will implement a comprehensive test suite using ScalaTest, Pekko HTTP Testkit, and Mockito (or similar) to cover all core components of the service.

### 1. Model Tests (`AddressSpec`)
- Verify JSON encoding/decoding for `Address` model (using Circe).
- Verify `Address` case class properties and defaults.

### 2. Auth Logic Tests (`JwtDirectivesSpec`)
- Test `authenticate` directive with valid/invalid/missing tokens.
- Test `authorizeRoles` directive with different user roles.
- Use a mock JWT generator for testing (Jose4j).

### 3. Route Tests (`AddressRoutesSpec`)
- Test all endpoints: `GET /api/addresses`, `GET /api/addresses/{id}`, `POST /api/addresses`, `DELETE /api/addresses/{id}`.
- Use Pekko HTTP Testkit for route testing.
- Mock `AddressRepository` to isolate route logic from MongoDB.
- Verify status codes (200, 201, 204, 404, 403, 401).

### 4. Repository Tests (`AddressRepositorySpec`)
- Although `AddressRepository` is a Spring Data interface, we can add a basic integration test if possible, or at least keep a simple test that ensures the repository is correctly picked up by Spring.
- Given the complexity of setting up Embedded MongoDB in this environment, we will focus on unit tests for logic and routes first.

### 5. Main Application Test (`MainSpec` / `ScalaAddressServiceConfigSpec`)
- Verify that the Spring context can be loaded.

## Verification Plan
1. Run `mvn clean verify -pl scala-address-service`.
2. Generate JaCoCo report and verify coverage for:
    - `com.microservice.address.auth.JwtDirectives`
    - `com.microservice.address.models.Address`
    - `com.microservice.address.routes.AddressRoutes`
    - `com.microservice.address.Main` (partial)
3. Ensure the `code-coverage` threshold in `scala-address-service/pom.xml` is updated to a reasonable value (e.g., 0.7 or 0.8).
