# Implementation Plan - Comprehensive Testing for scala-address-service

## Tasks

### 1. Preparation
- Update `scala-address-service/pom.xml` to include `mockito-scala` for easier mocking in Scala.

### 2. Model Tests (`AddressSpec.scala`)
- **Path**: `scala-address-service/src/test/scala/com/microservice/address/models/AddressSpec.scala`
- Test case class instantiation.
- Test JSON encoding/decoding using `AddressJsonProtocol`.

### 3. Auth Directive Tests (`JwtDirectivesSpec.scala`)
- **Path**: `scala-address-service/src/test/scala/com/microservice/address/auth/JwtDirectivesSpec.scala`
- Create a test class implementing `JwtDirectives`.
- Use `jose4j` to generate mock tokens.
- Test `authenticate` with valid, invalid, and missing tokens.
- Test `authorizeRoles` with various roles.

### 4. Route Tests (`AddressRoutesSpec.scala`)
- **Path**: `scala-address-service/src/test/scala/com/microservice/address/routes/AddressRoutesSpec.scala`
- Use `PekkoHttpTestkit`.
- Mock `AddressRepository` using `mockito-scala`.
- Verify each route:
    - `GET /api/addresses` (Admin access).
    - `GET /api/addresses/{id}` (Person/Admin access).
    - `POST /api/addresses` (Person/Admin access).
    - `DELETE /api/addresses/{id}` (Admin access).
- Test error cases (Not Found).
- Test auth rejection (401/403).

### 5. Repository Verification (`AddressRepositorySpec.scala`)
- Keep or refine existing test to ensure repository is loadable.

### 6. Verification and Cleanup
- Run `mvn clean verify -pl scala-address-service`.
- Review coverage report.
- Update `code-coverage` threshold in `scala-address-service/pom.xml` to `0.8`.
