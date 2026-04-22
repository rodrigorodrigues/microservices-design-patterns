# Logback AbstractMethodError Fix Design

## Problem Description
The project is experiencing a `java.lang.AbstractMethodError` at runtime. This error occurs in `ch.qos.logback.core.pattern.PatternLayoutBase.getEffectiveConverterMap`.
Investigation revealed a version mismatch in the `scala-address-service` module.

### Root Cause
- `scala-address-service/pom.xml` explicitly defines `ch.qos.logback:logback-classic` version `1.4.14`.
- The parent project (Spring Boot 4.0.1) manages `logback-classic` and `logback-core` versions to `1.5.22`.
- Maven dependency resolution results in `logback-classic:1.4.14` and `logback-core:1.5.22` being used together in `scala-address-service`.
- Logback 1.4.x and 1.5.x are binary incompatible in certain internal APIs used for pattern layout.

## Proposed Solution: Approach 1 (Align with Spring Boot)
The best way to fix this in a Spring Boot project is to remove the explicit version from the submodule and let the Spring Boot BOM manage it.

### Changes
- Modify `scala-address-service/pom.xml`:
    - Remove `<version>1.4.14</version>` from the `logback-classic` dependency.

### Verification Plan
1. **Dependency Verification**: Run `mvn dependency:tree` for `scala-address-service` and confirm that `logback-classic` and `logback-core` both resolve to `1.5.22`.
2. **Compilation**: Run `mvn clean compile -pl scala-address-service` to ensure no compile-time regressions.
3. **Execution**: Start the `scala-address-service` (or run its tests) to verify that the `AbstractMethodError` is resolved.

## Alternatives Considered
- **Approach 2**: Explicitly update to `1.5.22` in the submodule.
    - *Rejected*: Redundant with Spring Boot's management; prone to drift if Spring Boot is upgraded.
- **Approach 3**: Downgrade `logback-core` to `1.4.14`.
    - *Rejected*: Inconsistent with the rest of the microservices; potential issues with newer Spring Boot features.
