# Implementation Plan - Code Coverage for scala-address-service

Enabling automated code coverage reporting for the Scala service using JaCoCo.

## Proposed Changes

### Build Configuration

#### [scala-address-service/pom.xml]
- Add `jacoco-maven-plugin` to the `<build><plugins>` section.
- Set a local `<code-coverage>` property to `0.0` initially to ensure the build passes while we verify report generation, then adjust to a reasonable value based on current tests.

## Verification Plan

### Automated Tests
- Run `mvn clean test -pl scala-address-service`
- Verify that `scala-address-service/target/site/jacoco/index.html` exists.
- Run `mvn jacoco:check -pl scala-address-service` to verify threshold enforcement.

### Manual Verification
- Open the generated HTML report and verify it shows coverage for Scala classes in `src/main/scala`.
