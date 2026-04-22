# Design Spec - Code Coverage for scala-address-service

## Status
- **Date**: 2026-04-17
- **Status**: Draft
- **Author**: Junie

## Problem Statement
The `scala-address-service` currently lacks automated code coverage reporting. The root `pom.xml` has a `jacoco-maven-plugin` configuration in `pluginManagement`, but it is not activated for this specific service.

## Proposed Changes
We will activate JaCoCo for the `scala-address-service` by adding the plugin to its `pom.xml`.

### 1. Update `scala-address-service/pom.xml`
- Add `jacoco-maven-plugin` to the `<build><plugins>` section.
- It will inherit the configuration from the parent POM, including:
    - `prepare-agent` goal (runs before tests to instrument classes).
    - `report` goal (runs after tests to generate the HTML report).
    - `check` goal (enforces coverage thresholds).

### 2. Coverage Thresholds
- The parent POM defines `<code-coverage>0.8</code-coverage>`.
- The `scala-address-service` currently has very basic tests. We might need to adjust the threshold locally if 80% is too high for the initial setup, or add more tests to meet the requirement.
- Given the current state of `AddressRepositorySpec`, coverage will likely be low.

## Verification Plan
1. Run `mvn clean test -pl scala-address-service`.
2. Check if `scala-address-service/target/site/jacoco/index.html` is generated.
3. Verify that the coverage report correctly identifies covered and uncovered lines in the Scala source files.
4. Verify that `mvn check` fails if the threshold is not met (or passes if it is).

## Alternatives Considered
- **Scoverage**: A Scala-specific coverage tool. While it provides better Scala-specific insights, it would introduce a different toolchain compared to the rest of the project (which uses JaCoCo for Java/Kotlin). We prefer JaCoCo for consistency.
