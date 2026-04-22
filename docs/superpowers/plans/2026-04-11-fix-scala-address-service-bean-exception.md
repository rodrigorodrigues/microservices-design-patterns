# Fix BeanCreationException in scala-address-service Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Resolve `BeanCreationException` in `scala-address-service` by adding missing Spring Boot web infrastructure classes.

**Architecture:** Add `spring-boot-starter-webflux` to the project's dependencies to provide the `WebServerInitializedEvent` required by Spring Boot's autoconfiguration for MongoDB.

**Tech Stack:** Maven, Spring Boot 4.x, Scala 3.x

---

### Task 1: Add Dependency to pom.xml

**Files:**
- Modify: `scala-address-service/pom.xml`

- [ ] **Step 1: Add spring-boot-starter-webflux dependency**

Add the following dependency to the `<dependencies>` section of `scala-address-service/pom.xml`:

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
```

- [ ] **Step 2: Commit changes**

```bash
git add scala-address-service/pom.xml
git commit -m "fix: add spring-boot-starter-webflux to resolve BeanCreationException" --trailer "Co-authored-by: Junie <junie@jetbrains.com>"
```

### Task 2: Verify Fix

**Files:**
- Test: `scala-address-service/src/test/scala/com/microservice/address/repository/AddressRepositorySpec.scala`

- [ ] **Step 1: Compile the module**

Run: `mvn clean compile -pl scala-address-service`
Expected: SUCCESS

- [ ] **Step 2: Run existing tests**

Run: `mvn test -pl scala-address-service`
Expected: Tests should pass (this confirms that Spring context can be loaded in tests).

- [ ] **Step 3: Manual Startup Verification**

Since we cannot easily run the full microservice ecosystem in this environment, we will verify that the Spring context can be initialized.
Run the following command to attempt to start the application (it might fail later due to missing MongoDB, but we check if `BeanCreationException` for `mongoMappingContext` is gone):

```bash
mvn spring-boot:run -pl scala-address-service -Dspring-boot.run.arguments="--server.port=0 --spring.mongodb.embedded.storage.enabled=false"
```
Wait for output and check if `BeanCreationException` related to `WebServerInitializedEvent` is gone. If it fails with `MongoTimeoutException` or similar, it means the bean was created successfully.

- [ ] **Step 4: Final Check**

Ensure no other regressions were introduced.
