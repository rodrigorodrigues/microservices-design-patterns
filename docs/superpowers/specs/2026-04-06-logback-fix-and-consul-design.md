# Logback Fix and Consul Registration for scala-address-service

## Overview
This design addresses two requirements for the `scala-address-service`:
1. Fix a `java.lang.AbstractMethodError` caused by a Logback version mismatch.
2. Implement Consul registration and configuration, mirroring the `person-service` implementation.

## 1. Logback AbstractMethodError Fix

### Problem
`scala-address-service/pom.xml` explicitly defines `ch.qos.logback:logback-classic:1.4.14`. However, Spring Boot 4.0.1 manages `logback-core` to `1.5.22`. These versions are binary incompatible for some internal layout APIs.

### Solution
Remove the explicit version of `logback-classic` in `scala-address-service/pom.xml`. This allows the Spring Boot BOM to manage both `logback-classic` and `logback-core` to version `1.5.22`, ensuring compatibility.

## 2. Consul Registration

### Requirements
Mirror the `person-service` Consul implementation in `scala-address-service`.

### Changes to scala-address-service/pom.xml
Add the following dependencies to support Consul Config and Discovery:
- `org.springframework.cloud:spring-cloud-starter-consul-config`
- `org.springframework.cloud:spring-cloud-starter-consul-discovery`
- `org.springframework.boot:spring-boot-starter-actuator` (needed for health checks)

### Configuration (application.yml)
Create `scala-address-service/src/main/resources/application.yml` with the following configuration:
- Application name: `scala-address-service`
- Consul profile settings (import consul config, disable `ConsulConfigAutoConfiguration` in exclude if needed).
- Discovery settings (enabled by default when the starter is present).

### Code Changes
The `Main.scala` already uses `SpringApplication.run`, which will initialize the Spring context and trigger Consul registration if the dependencies and configuration are present.

## Verification Plan
1. **Logback Fix**: Run `mvn dependency:tree -pl scala-address-service` and verify that both `logback-classic` and `logback-core` resolve to `1.5.22`.
2. **Consul Registration**: 
    - Verify that `scala-address-service` starts up and logs Consul registration.
    - If a Consul agent is running, verify it appears in the Consul UI.
3. **Compilation**: Ensure `mvn clean compile -pl scala-address-service` passes.
