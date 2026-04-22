# Design Doc - GitHub Actions for scala-address-service

Create a GitHub Actions workflow for building and pushing Docker images for `scala-address-service`, based on the `person-service` workflow.

## Problem Statement

The `scala-address-service` lacks an automated CI/CD pipeline for building and pushing its Docker image to DockerHub and ECR, and deploying it to ECS.

## Proposed Changes

### 1. `scala-address-service/pom.xml`
- Add `io.fabric8:docker-maven-plugin` configuration.
- Configure it to build a Docker image using the base image defined in the parent POM (`${docker.image.from.fabric8}`).
- Set up the assembly to include the project artifact.
- Define the command to run the Scala service.

### 2. `.github/.aws/scala-address-service-task-definition.json`
- Create a new ECS task definition for `scala-address-service`.
- Based on `person-service-task-definition.json`.
- Update family, container name, image, and port (the service likely uses a different port, will use 8080 as default for Pekko if not specified, but will check `Main.scala` if possible).

### 3. `.github/workflows/docker-build-push-image-scala-address-service.yml`
- Create the workflow file.
- Update `IMAGE`, `ECS_SERVICE`, `ECS_TASK_DEFINITION`, `CONTAINER_NAME` environment variables.
- Update the build step to target `scala-address-service`.
- Update the Docker push step to use the correct context and tags.
- Update the `if` condition to trigger on `build scala-address-service`.

## Verification Plan

- Validate `pom.xml` by running `mvn help:effective-pom -pl scala-address-service` to ensure the plugin is correctly inherited and configured.
- Check the syntax of the new workflow file.
- Verify the task definition JSON structure.
