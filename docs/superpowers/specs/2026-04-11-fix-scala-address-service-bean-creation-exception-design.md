# Design Spec: Fix BeanCreationException in scala-address-service

## 1. Problem Description
The `scala-address-service` is failing to start with a `BeanCreationException` during the initialization of the `mongoMappingContext` bean.

### 1.1 Root Cause
The error `Type org.springframework.boot.web.server.context.WebServerInitializedEvent not present` indicates that a class required by Spring Boot's MongoDB autoconfiguration is missing from the classpath.

Although `scala-address-service` uses Pekko HTTP for its web server, it relies on Spring Boot's autoconfiguration for MongoDB, Actuator, and Cloud Consul. The version of Spring Boot used (4.0.1) and its associated MongoDB autoconfiguration (specifically `DataMongoConfiguration`) expect Spring Boot web classes to be present.

## 2. Approach: Add spring-boot-starter-webflux
We will add `spring-boot-starter-webflux` to the `scala-address-service/pom.xml` file.

### 2.1 Why WebFlux?
- The service already uses `spring-boot-starter-data-mongodb-reactive`, making the reactive web starter a natural fit.
- It provides the necessary classes for Spring Boot's web infrastructure, satisfying the autoconfiguration's requirements.
- It is consistent with other services in the project that use WebFlux.

## 3. Design
The only change is the addition of a new dependency in `scala-address-service/pom.xml`.

### 3.1 Files to Modify
- `scala-address-service/pom.xml`

### 3.2 Dependency Details
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

## 4. Verification Plan
1. **Compilation Check**: Ensure the project still builds successfully with `mvn clean compile -pl scala-address-service`.
2. **Application Startup**: Attempt to run the service and verify that the `BeanCreationException` is no longer present and the application starts correctly.
3. **Existing Tests**: Run existing tests in `scala-address-service` to ensure no regressions.
