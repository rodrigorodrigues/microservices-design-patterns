# Logback Fix and Consul Registration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the Logback `AbstractMethodError` and implement Consul registration for the `scala-address-service` module.

**Architecture:** 
1. Align Logback versions by removing the explicit version from `scala-address-service/pom.xml`.
2. Add Spring Cloud Consul dependencies and Spring Boot Actuator to `scala-address-service/pom.xml`.
3. Create an `application.yml` for `scala-address-service` with Consul configuration mirroring `person-service`.

**Tech Stack:** Maven, Spring Boot 4.0.1, Spring Cloud 2025.1.0, Logback, Consul.

---

### Task 1: Fix Logback Version Mismatch

**Files:**
- Modify: `scala-address-service/pom.xml`

- [ ] **Step 1: Remove explicit logback-classic version**

```xml
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <!-- REMOVE THIS LINE: <version>1.4.14</version> -->
</dependency>
```

- [ ] **Step 2: Verify dependency resolution**

Run: `mvn dependency:tree -pl scala-address-service -Dincludes=ch.qos.logback`
Expected: Both `logback-classic` and `logback-core` should resolve to `1.5.22`.

- [ ] **Step 3: Commit**

```bash
git add scala-address-service/pom.xml
git commit -m "fix: align logback version in scala-address-service" --trailer "Co-authored-by: Junie <junie@jetbrains.com>"
```

---

### Task 2: Add Consul Dependencies

**Files:**
- Modify: `scala-address-service/pom.xml`

- [ ] **Step 1: Add Consul and Actuator dependencies**

Add these inside the `<dependencies>` section:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-consul-config</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-consul-discovery</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

- [ ] **Step 2: Verify compilation**

Run: `mvn clean compile -pl scala-address-service`
Expected: SUCCESS

- [ ] **Step 3: Commit**

```bash
git add scala-address-service/pom.xml
git commit -m "feat: add consul and actuator dependencies to scala-address-service" --trailer "Co-authored-by: Junie <junie@jetbrains.com>"
```

---

### Task 3: Configure Consul Registration

**Files:**
- Create: `scala-address-service/src/main/resources/application.yml`

- [ ] **Step 1: Create application.yml with Consul configuration**

```yaml
spring:
  application:
    name: scala-address-service
  main:
    allow-bean-definition-overriding: true
  cloud:
    consul:
      discovery:
        instance-id: ${spring.application.name}:${random.value}
        health-check-path: /actuator/health
        health-check-interval: 10s
        register: true
---
spring:
  config:
    activate:
      on-profile: consul
    import: consul:${consul_url:localhost:8500}
  cloud:
    consul:
      config:
        fail-fast: ${FAIL_FAST:true}
        format: yaml
  autoconfigure:
    exclude: org.springframework.cloud.consul.config.ConsulConfigAutoConfiguration

management:
  endpoints:
    web:
      exposure:
        include: 'health,info,prometheus'
```

- [ ] **Step 2: Verify project build**

Run: `mvn clean install -pl scala-address-service -DskipTests`
Expected: SUCCESS

- [ ] **Step 3: Commit**

```bash
git add scala-address-service/src/main/resources/application.yml
git commit -m "feat: configure consul registration for scala-address-service" --trailer "Co-authored-by: Junie <junie@jetbrains.com>"
```

---

### Task 4: Final Verification

- [ ] **Step 1: Check runtime dependencies**

Run: `mvn dependency:tree -pl scala-address-service`
Verify no 1.4.x logback remains.

- [ ] **Step 2: Dry-run startup (optional/manual)**

Ensure the service starts without the AbstractMethodError.
