# Plan - GitHub Actions for scala-address-service

Create a GitHub Actions workflow for building and pushing Docker images for `scala-address-service`.

## Tasks

### 1. Update `scala-address-service/pom.xml`
- Add `io.fabric8:docker-maven-plugin` configuration.
- Path: `scala-address-service/pom.xml`

### 2. Create ECS Task Definition
- Create `.github/.aws/scala-address-service-task-definition.json`.
- Set port to 8085.
- Update family and container names.

### 3. Create GitHub Actions Workflow
- Create `.github/workflows/docker-build-push-image-scala-address-service.yml`.
- Configure environment variables and build steps.

### 4. Verification
- Run `mvn help:effective-pom -pl scala-address-service` to verify plugin configuration.
- Perform a dry run of the build if possible.
